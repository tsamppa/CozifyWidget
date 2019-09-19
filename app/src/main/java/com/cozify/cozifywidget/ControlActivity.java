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
    private boolean mUpdating = false;
    private boolean mIsArming = false;
    private boolean mIsArmed = false;
    private boolean mIsControlling = false;
    private boolean mArmedForDesiredState = false;
    private String mDeviceId;
    private static Handler handler = new Handler();
    private Runnable delayedDisarm = null;

    private CozifySceneOrDeviceStateManager stateMgr;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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
        if (stateMgr == null) {
            stateMgr = new CozifySceneOrDeviceStateManager(this, mAppWidgetId);
        }

        Intent resultValue = new Intent();
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
        setResult(RESULT_CANCELED, resultValue);

        if (!loadSavedSettings()) {
            updateDeviceState("onCreate()");
        } else {
            if (handleClick()) {
                setResult(RESULT_OK, resultValue);
            }
        }
        finish();
    }

    private boolean handleToggleClick() {
        if (mIsControlling || mIsArming || mUpdating) {
            return true;
        } else {
            if (isArmed()) {
                cancelDelayedDisarm();
                if (toggleOnOff()) {
                    return true;
                } else {
                    updateDeviceState("handleToggleClick()");
                }
            } else {
                updateDeviceStateAndArm();
                return true;
            }
        }
        return false;
    }

    private boolean handleMeasurementUpdate() {
        updateDeviceState("handleMeasurementUpdate()");
        return true;
    }

    private boolean handleClick() {
        CozifySceneOrDeviceState state = stateMgr.getCurrentState();
        if (state != null) {
            if (!state.initialized) {
                updateDeviceState("handleClick().!state.initialized");
            }
            if (state.isOnOff()) {
                return handleToggleClick();
            }
            if (state.hasMeasurement()) {
                return handleMeasurementUpdate();
            }
        } else {
            updateDeviceState("handleClick().state==null");
        }
        return false;
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

    private void arm(boolean armForOnOff) {
        mIsArmed = true;
        mIsArming = false;
        mArmedForDesiredState = armForOnOff;
        saveSettings();
        ShowMessage("Press again in 5s to control " + (mArmedForDesiredState ? "ON" : "OFF"));
        displayDeviceState("arm");
        delayedDisarm = new Runnable() {
            @Override
            public void run() {
                if (mIsArmed) {
                    mIsArmed = false;
                    saveSettings();
                    if (!(mIsControlling || mIsArming) ) {
                        updateDeviceState("delayedDisarm.!mIsControlling");
                    }
                    displayDeviceState("delayedDisarm");
                }
            }
        };
        handler.postDelayed(delayedDisarm, 5000);
    }

    private void cancelDelayedDisarm() {
        handler.removeCallbacksAndMessages(null);
    }

    private void disarm() {
        cancelDelayedDisarm();
        if (delayedDisarm != null) {
            handler.removeCallbacks(delayedDisarm);
            delayedDisarm = null;
        }
        mIsArmed = false;
        saveSettings();
        displayDeviceState("disarm");
    }

    private void saveSettings() {
        if (!PersistentStorage.getInstance().saveSettings(this, mAppWidgetId, stateMgr.isOn(), mIsArmed, mIsArming, mArmedForDesiredState, mIsControlling, stateMgr.isReachable())) {
           ShowErrorMessage("Saving settings failed", "SharedPreferences returned failure in commit() for setting.");
        }
        stateMgr.saveState();
    }

    private boolean loadSavedSettings() {
        if (mAppWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            ShowMessage("INVALID_APPWIDGET_ID : Loading settings failed!");
            return false;
        }
        final Context context = ControlActivity.this;

        JSONObject settings = PersistentStorage.getInstance().loadSettingsJson(context, mAppWidgetId);
        if (settings == null) {
            ShowErrorMessage("FAILED to load settings", "PersistentStorage returned null.");
            return false;
        }
        try {
            mIsArmed = settings.getBoolean("armed");
            mArmedForDesiredState = settings.getBoolean("armedForDesiredState");
            mIsControlling = settings.getBoolean("controlling");
            if (mIsControlling) {
                mIsControlling = !stateMgr.isReady();
            }
            mIsArming  = settings.getBoolean("arming");
        } catch (JSONException e) {
            ShowErrorMessage("FAILED to load settings", e.getMessage());
        }

        mDeviceId = PersistentStorage.getInstance().loadDeviceId(this, mAppWidgetId);
        if (mDeviceId == null) {
            ShowErrorMessage("Configuration issue","Stored device ID not found (null). Please remove and recreate the device widget");
            return false;
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

    private void displayDeviceState(String why) {
        final Context context = ControlActivity.this;
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
        String device_name = PersistentStorage.getInstance().loadDeviceName(context, mAppWidgetId);
        float textSize = PersistentStorage.getInstance().loadTextSize(context, mAppWidgetId);
        int resourceForState = updateDeviceState(device_name, mIsControlling, mIsArming, mIsArmed, mUpdating,
                stateMgr, textSize, mAppWidgetId, appWidgetManager, this.getPackageName());
        Log.d("ResourceForState", String.format("%s : %s (%d)", why, getResources().getResourceEntryName(resourceForState), resourceForState));
    }

    public static int updateDeviceState(String device_name, boolean isControlling, boolean isArming, boolean isArmed, boolean isUpdating,
                                        CozifySceneOrDeviceStateManager stateMgr,
                                        float textSize,
                                        int appWidgetId, AppWidgetManager appWidgetManager, String packageName) {

        RemoteViews views = new RemoteViews(packageName, R.layout.appwidget_button);
        views.setBoolean(R.id.control_button, "setEnabled", !(isControlling || isArming || isUpdating));
        String measurement = stateMgr.getMeasurementString();
        CozifySceneOrDeviceState s = stateMgr.getCurrentState();
        boolean isSensor = s.hasMeasurement() && !s.isOnOff();
        int resourceForState = getDeviceResourceForState(stateMgr.isReachable(), stateMgr.isOn(), isArmed, isArming, isControlling, isSensor, isUpdating);
        views.setInt(R.id.control_button, "setBackgroundResource", resourceForState);

        if (measurement != null) {
            String label = measurement;
                if (device_name != null) {
                    label = measurement + "\n" + device_name;
                }
                views.setCharSequence(R.id.control_button, "setText", label);
                views.setFloat(R.id.control_button, "setTextSize", textSize);
        } else {
            if (device_name != null) {
                views.setCharSequence(R.id.control_button, "setText", device_name);
            }
        }
        appWidgetManager.updateAppWidget(appWidgetId, views);
        return resourceForState;
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
        displayDeviceState("endControl");
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

    private void updateDeviceState(String why) {
        if (mUpdating) return;
        mUpdating = true;
        displayDeviceState(why + ".updateDeviceState()");
        if (mDeviceId == null) {
            ShowMessage("Configuration issue. Stored device not found (null). Please remove and recreate the device widget");
            return;
        }

        stateMgr.updateCurrentState(mDeviceId, new CozifyApiReal.CozifyCallback() {
            @Override
            public void result(boolean success, String status, JSONObject resultJson, JSONObject requestJson) {
                mUpdating = false;
                if (success) {
                    loadSavedSettings();
                    CozifySceneOrDeviceState state = stateMgr.getCurrentState();
                    if (state == null) {
                        return;
                    }
                } else {
                    stateMgr.setReachable(false);
                    if (!stateMgr.connected) {
                        ShowErrorMessage("Login expired. Please login again.", "!stateMgr.connected in ControlActivity.updateCurrentState()");
                        loginAgain();
                    }
                }
                saveSettings();
                displayDeviceState("updateDeviceState().stateMgr.updateCurrentState");
            }
        });
    }

    private void loginAgain() {
        Intent intent = new Intent(this, CozifyWidgetSetupActivity.class);
        startActivity(intent);
    }

    private void updateDeviceStateAndArm() {
        if (mDeviceId == null) {
            ShowMessage("Configuration issue. Stored device not found (null). Please remove and recreate the device widget");
            return;
        }
        mIsArming = true;
        displayDeviceState("updateDeviceStateAndArm");
        stateMgr.updateCurrentState(mDeviceId, new CozifyApiReal.CozifyCallback() {
            @Override
            public void result(boolean success, String status, JSONObject resultJson, JSONObject requestJson) {
                if (success) {
                    loadSavedSettings();
                    CozifySceneOrDeviceState state = stateMgr.getCurrentState();
                    arm(!state.isOn);
                } else {
                    stateMgr.setReachable(false);
                    mIsArming = false;
                    saveSettings();
                    displayDeviceState("updateDeviceStateAndArm().stateMgr.updateCurrentState");
                }
            }
        });
    }


    private boolean toggleOnOff() {
        startControl();
        if (mDeviceId == null) {
            endControl(false, "Configuration issue. Stored device not found (null). Please remove and recreate the device widget");
            return false;
        }
        controlToggle();
        return true;
    }

    void controlToggle() {
        stateMgr.controlStateToDesired(mDeviceId, mArmedForDesiredState, new CozifyApiReal.CozifyCallback() {
            @Override
            public void result(boolean success, String status, JSONObject jsonResponse, JSONObject jsonRequest) {
                if (success) {
                    endControl(true, status);
                } else {
                    stateMgr.setReachable(false);
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

