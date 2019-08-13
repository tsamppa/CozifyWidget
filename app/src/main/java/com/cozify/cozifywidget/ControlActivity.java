package com.cozify.cozifywidget;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONException;
import org.json.JSONObject;

public class ControlActivity extends AppCompatActivity {
    private int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
    private TextView textViewStatus;
    private long lastStateUpdate = 0;
    private boolean mUpdating = false;
    private boolean mIsArming = false;
    private boolean mIsArmed = false;
    private boolean mIsOn = false;
    private boolean mIsControlling = false;
    private boolean mIsReachable = true;
    private String mDeviceId;
    private CozifyAPI cozifyAPI = CozifyApiReal.getInstance();
    private Handler handler = new Handler();
    private Runnable delayedDisarm = null;

    private CozifySceneOrDeviceStateManager stateMgr;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (stateMgr == null) stateMgr = new CozifySceneOrDeviceStateManager();

        // Set the result to CANCELED.  This will cause the widget host to cancel
        // out of the widget placement if they press the back button.
        setResult(RESULT_CANCELED);
        setContentView(R.layout.activity_control);
        initViews();

        if (!initWidgetId()) {
            ShowMessage("WidgetId not found. Reinstall this widget.");
            finish();
            return;
        }

        Intent resultValue = new Intent();
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
        setResult(RESULT_CANCELED, resultValue);

        if (!loadSavedSettings(true)) {
            updateDeviceState();
        } else {
            if (handleClick()) {
                setResult(RESULT_OK, resultValue);
            }
        }
        finish();
    }

    private boolean handleToggleClick() {
        if (mIsControlling || mIsArming) {
            return true;
        } else {
            if (isArmed()) {
                if (toggelOnOff()) {
                    return true;
                } else {
                    updateDeviceState();
                }
            } else {
                updateDeviceStateAndArm();
                return true;
            }
        }
        return false;
    }

    private boolean handleMeasurementUpdate() {
        updateDeviceState();
        return true;
    }

    private boolean handleClick() {
        CozifySceneOrDeviceState state = stateMgr.getCurrentState();
        if (state != null) {
            if (!state.initialized) {
                updateDeviceState();
            }
            if (state.isOnOff()) {
                return handleToggleClick();
            }
            if (state.hasMeasurement()) {
                return handleMeasurementUpdate();
            }
        } else {
            updateDeviceState();
        }
        return false;
    }

    private void setIsOn(boolean isOn, String saysWho) {
        mIsOn = isOn;
        //Log.d("ICON STATE", " " + (isOn ? "ON" : "OFF") + " by " + saysWho);
    }

    private void ShowMessage(String message) {
        setStatusMessage(message);
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        Log.i("Widget:"+mAppWidgetId, message);
    }

    private void ShowErrorMessage(String errorMessage, String details) {
        ShowMessage(errorMessage);
        Log.e("Widget: "+mAppWidgetId+" ERROR details:", details);
    }

    private boolean isArmed() {
        return mIsArmed;
    }

    private void arm() {
        mIsArmed = true;
        mIsArming = false;
        saveSettings();
        ShowMessage("Press again in 5s to control " + (mIsOn?"OFF":"ON"));
        displayDeviceState();
        delayedDisarm = new Runnable() {
            @Override
            public void run() {
                if (mIsArmed) {
                    mIsArmed = false;
                    saveSettings();
                    if (!mIsControlling) {
                        updateDeviceState();
                    }
                    displayDeviceState();
                }
            }
        };
        handler.postDelayed(delayedDisarm, 5000);
    }

    private void disarm() {
        if (delayedDisarm != null) {
            handler.removeCallbacks(delayedDisarm);
            delayedDisarm = null;
        }
        mIsArmed = false;
        saveSettings();
        displayDeviceState();
    }

    private void saveSettings() {
        if (!PersistentStorage.getInstance().saveSettings(this, mAppWidgetId, mIsOn, mIsArmed, mIsArming, mIsControlling, mIsReachable)) {
           ShowErrorMessage("Saving settings failed", "SharedPreferences returned failure in commit() for setting.");
        }
        if (!PersistentStorage.getInstance().saveState(this, mAppWidgetId, stateMgr.getCurrentState())) {
            ShowErrorMessage("Saving settings failed", "SharedPreferences returned failure in commit() for state.");
        }
    }

    private boolean loadSavedSettings(boolean loadStateToo) {
        if (mAppWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            ShowMessage("INVALID_APPWIDGET_ID : Loading settings failed!");
            return false;
        }
        final Context context = ControlActivity.this;
        String cloudToken = PersistentStorage.getInstance().loadCloudToken(context);
        if (cloudToken != null && cloudToken.length() > 0)
            cozifyAPI.setCloudToken(cloudToken);
        String hubKey = PersistentStorage.getInstance().loadHubKey(context);
        if (hubKey != null && hubKey.length() > 0)
            cozifyAPI.setHubKey(hubKey);
        JSONObject settings = PersistentStorage.getInstance().loadSettingsJson(context, mAppWidgetId);
        if (settings == null) {
            ShowErrorMessage("FAILED to load settings", "PersistentStorage returned null.");
            return false;
        }
        try {
            mIsArmed = settings.getBoolean("armed");
            setIsOn(settings.getBoolean("isOn"), "loadSavedSettings()");
            mIsControlling = settings.getBoolean("controlling");
            mIsReachable = settings.getBoolean("reachable");
            mIsArming  = settings.getBoolean("arming");
        } catch (JSONException e) {
            ShowErrorMessage("FAILED to load settings", e.getMessage());
        }

        mDeviceId = PersistentStorage.getInstance().loadDeviceId(this, mAppWidgetId);
        if (mDeviceId == null) {
            ShowErrorMessage("Configuration issue","Stored device ID not found (null). Please remove and recreate the device widget");
            return false;
        }
        if (loadStateToo) {
            CozifySceneOrDeviceState state = PersistentStorage.getInstance().loadState(this, mAppWidgetId);
            if (state == null) {
                return false;
            }
            if (state.initialized) {
                if (state.id.equals(mDeviceId)) {
                    stateMgr.setCurrentState(state);
                } else {
                    ShowErrorMessage("FAILED to load settings", "Mismatching deviceId in saved state!");
                }
            }
        }
        return true;
    }

    private boolean initWidgetId() {
        mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
        Intent intent = this.getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            mAppWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        }
        if (mAppWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            mAppWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        }

        if (mAppWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            ShowMessage("ERROR: appWidgetId not found in extra (INVALID_APPWIDGET_ID)");
            finish();
            return false;
        }
        return true;
    }

    private void initViews() {
        textViewStatus = findViewById(R.id.control_status);
    }

    private void setStatusMessage(String statusMessage) {
        if (textViewStatus != null)
            textViewStatus.setText(statusMessage);
    }

    static public int getDeviceResourceForState(boolean isReachable, boolean isOn, boolean isArmed, boolean isArming, boolean isControlling, boolean isSensor, boolean isUpdating) {
        int resourceForState = isOn ? R.drawable.appwidget_button_clickable_on : R.drawable.appwidget_button_clickable_off;
        if (isReachable) {
            if (isOn) {
                if (isControlling) {
                    // Controlling towards Off
                    resourceForState = R.drawable.appwidget_button_controlling_off;
                } else if (isArmed) {
                    // Armed towards Off
                    resourceForState = R.drawable.appwidget_button_armed_on;
                } else if (isArming) {
                    resourceForState = R.drawable.appwidget_button_arming_on;
                } else if (isSensor) {
                    if (isUpdating) {
                        resourceForState = R.drawable.appwidget_sensor_updating;
                    } else {
                        resourceForState = R.drawable.appwidget_sensor_clickable;
                    }
                }
            } else {
                if (isControlling) {
                    // Controlling towards On
                    resourceForState = R.drawable.appwidget_button_controlling_on;
                } else if (isArmed) {
                    // Armed towards On
                    resourceForState = R.drawable.appwidget_button_armed_off;
                } else if (isArming) {
                    resourceForState = R.drawable.appwidget_button_arming_off;
                } else if (isSensor) {
                    if (isUpdating) {
                        resourceForState = R.drawable.appwidget_sensor_updating;
                    } else {
                        resourceForState = R.drawable.appwidget_sensor_clickable;
                    }
                }
            }
        } else if (isControlling) {
            if (isOn) {
                // Controlling unreachable towards Off
                resourceForState = R.drawable.appwidget_button_controlling_unreachable_off;
            } else {
                // Controlling unreachable towards On
                resourceForState = R.drawable.appwidget_button_controlling_unreachable_on;
            }
        } else if (isArmed) {
            // Unreachable but armed
            if (isOn) {
                resourceForState = R.drawable.appwidget_button_armed_unreachable_on;
            } else {
                // Controlling unreachable towards On
                resourceForState = R.drawable.appwidget_button_armed_unreachable_off;
            }
        } else if (isArming) {
            // Arming unreachable
            if (isOn) {
                resourceForState = R.drawable.appwidget_button_arming_unreachable_on;
            } else {
                // Controlling unreachable towards On
                resourceForState = R.drawable.appwidget_button_arming_unreachable_off;
            }
        } else if (isSensor) {
            if (isUpdating) {
                resourceForState = R.drawable.appwidget_sensor_updating_unreachable;
            } else {
                resourceForState = R.drawable.appwidget_sensor_clickable_unreachable;
            }
        } else {
            // Unreachable
            if (isOn) {
                resourceForState = R.drawable.appwidget_button_unreachable_on;
            } else {
                // Controlling unreachable towards On
                resourceForState = R.drawable.appwidget_button_unreachable_off;
            }
        }
        return resourceForState;
    }

    private void displayDeviceState() {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
        RemoteViews views = new RemoteViews(this.getPackageName(), R.layout.demo_app_widget);
        views.setBoolean(R.id.control_button, "setEnabled", !mIsControlling);

        String measurement = stateMgr.getMeasurementString();
        boolean isSensor = measurement != null;

        int resourceForState = getDeviceResourceForState(mIsReachable, mIsOn, mIsArmed, mIsArming, mIsControlling, isSensor, mUpdating);
        views.setInt(R.id.control_button, "setBackgroundResource", resourceForState);

        if (measurement != null) {
            final Context context = ControlActivity.this;
            String device_name = PersistentStorage.getInstance().loadDeviceName(context, mAppWidgetId);
            if (device_name != null) {
                String label = measurement + "\n" + device_name;
                views.setCharSequence(R.id.control_button, "setText", label);
            }
        }

        appWidgetManager.updateAppWidget(mAppWidgetId, views);
    }

    private void startControl() {
        mIsControlling = true;
        saveSettings();
        disarm();
        ShowMessage("Sending control command..");
    }

    private void endControl(boolean success, String reason) {
        mIsControlling = false;
        saveSettings();
        displayDeviceState();
        if (success) {
            ShowMessage("Control OK");
        } else {
            ShowErrorMessage("Control FAILED!", reason);
        }
    }

    private void endControl(boolean success, String reason, String details) {
        if (details != null)
            Log.e("WidgetEndControl", details);
        endControl(success, reason);
    }

    private void updateDeviceState() {
        if (mUpdating) return;
        mUpdating = true;
        displayDeviceState();
        if (mDeviceId == null) {
            ShowMessage("Configuration issue. Stored device not found (null). Please remove and recreate the device widget");
            return;
        }

        stateMgr.updateCurrentState(mDeviceId, new CozifyAPI.CozifyCallback() {
            @Override
            public void result(boolean success, String status, JSONObject resultJson, JSONObject requestJson) {
                mUpdating = false;
                if (success) {
                    loadSavedSettings(false);
                    lastStateUpdate = System.currentTimeMillis();
                    CozifySceneOrDeviceState state = stateMgr.getCurrentState();
                    setIsOn(state.isOn, "updateDeviceState()");
                    mIsReachable = state.reachable;
                } else {
                    mIsReachable = false;
                }
                saveSettings();
                displayDeviceState();
            }
        });
    }

    private void updateDeviceStateAndArm() {
        if (mDeviceId == null) {
            ShowMessage("Configuration issue. Stored device not found (null). Please remove and recreate the device widget");
            return;
        }
        mIsArming = true;
        displayDeviceState();
        stateMgr.updateCurrentState(mDeviceId, new CozifyAPI.CozifyCallback() {
            @Override
            public void result(boolean success, String status, JSONObject resultJson, JSONObject requestJson) {
                if (success) {
                    loadSavedSettings(false);
                    lastStateUpdate = System.currentTimeMillis();
                    CozifySceneOrDeviceState state = stateMgr.getCurrentState();
                    setIsOn(state.isOn, "updateDeviceState()");
                    mIsReachable = state.reachable;
                    arm();
                } else {
                    mIsReachable = false;
                    mIsArming = false;
                    saveSettings();
                    displayDeviceState();
                }
            }
        });
    }


    private boolean toggelOnOff() {
        startControl();
        if (mDeviceId == null) {
            endControl(false, "Configuration issue. Stored device not found (null). Please remove and recreate the device widget");
            return false;
        }
        controlToggle();
        return true;
    }

    void controlToggle() {
        stateMgr = new CozifySceneOrDeviceStateManager();
        stateMgr.toggleState(mDeviceId, new CozifyAPI.CozifyCallback() {
            @Override
            public void result(boolean success, String status, JSONObject jsonResponse, JSONObject jsonRequest) {
                if (success) {
                    setIsOn(stateMgr.getCurrentState().isOn, "controlToggle()");
                    mIsReachable = stateMgr.getCurrentState().reachable;
                    saveSettings();
                    endControl(true, status);
                } else {
                    mIsReachable = false;
                    String details = "Details:";
                    if (jsonResponse != null)
                        details += " jsonResponse: "+jsonResponse.toString();
                    if (jsonRequest != null)
                        details += " jsonRequest: "+jsonRequest.toString();
                    endControl(false, status, details);
                }
            }
        });
    }
}

