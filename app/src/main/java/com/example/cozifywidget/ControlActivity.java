package com.example.cozifywidget;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

public class ControlActivity extends AppCompatActivity {
    private int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
    private TextView textViewStatus;
    private boolean mIsArmed = false;
    private boolean mIsOn = false;
    private boolean mIsControlling = false;
    private boolean mIsReachable = true;
    private static CozifyAPI cozifyAPI = CozifyApiReal.getInstance();

    private CozifySceneOrDeviceStateManager stateMgr;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initViews();

        // Set the result to CANCELED.  This will cause the widget host to cancel
        // out of the widget placement if they press the back button.
        setResult(RESULT_CANCELED);
        setContentView(R.layout.activity_control);

        if (!initWidgetId()) {
            ShowMessage("WidgetId not found. Reinstall this widget.");
            finish();
            return;
        }

        Intent resultValue = new Intent();
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
        setResult(RESULT_CANCELED, resultValue);

        loadSavedSettings();

        if (isArmed()) {
            if (toggelOnOff()) {
                disarm();
                setResult(RESULT_OK, resultValue);
            } else {
                updateDeviceState();
            }
        } else {
            updateDeviceState();
            arm();
            setResult(RESULT_OK, resultValue);
        }
        finish();
    }

    private void ShowMessage(String message) {
        setStatusMessage(message);
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        Log.w("Widget", message);
    }

    private void ShowMessage(String message, String details) {
        ShowMessage(message);
        Log.w("Widget", details);
    }

    private boolean isArmed() {
        return mIsArmed;
    }

    private void arm() {
        mIsArmed = true;
        saveSettings();
        ShowMessage("Press again to control");
        displayDeviceState();
    }

    private void disarm() {
        mIsArmed = false;
        saveSettings();
        displayDeviceState();
    }

    private void saveSettings() {
        PersistentStorage.getInstance().saveSettings(this, mAppWidgetId, mIsOn, mIsArmed);
    }

    private void loadSavedSettings() {
        final Context context = ControlActivity.this;
        String cloudToken = PersistentStorage.getInstance().loadCloudToken(context);
        if (cloudToken != null && cloudToken.length() > 0)
            cozifyAPI.setCloudToken(cloudToken);
        String hubKey = PersistentStorage.getInstance().loadHubKey(context);
        if (hubKey != null && hubKey.length() > 0)
            cozifyAPI.setHubKey(hubKey);
        String statusJson = PersistentStorage.getInstance().loadSettings(context, mAppWidgetId);
        if (statusJson != null && statusJson.length() > 0) {
            try {
                JSONObject json = new JSONObject(statusJson);
                mIsArmed = json.getBoolean("armed");
                mIsOn = json.getBoolean("isOn");
            } catch (JSONException e) {
                ShowMessage("FAILED to load settings: " + e.getMessage());
            }
        }
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

    static public int getDeviceResourceForState(boolean isReachable, boolean isOn, boolean isArmed, boolean isControlling) {
        int resourceForState = isOn ? R.drawable.appwidget_button_clickable_on : R.drawable.appwidget_button_clickable_off;
        if (isReachable) {
            if (isOn) {
                if (isControlling) {
                    // Controlling towards Off
                    resourceForState = R.drawable.appwidget_button_controlling_off;
                } else if (isArmed) {
                    // Armed towards Off
                    resourceForState = R.drawable.appwidget_button_armed_on;
                }
            } else {
                if (isControlling) {
                    // Controlling towards On
                    resourceForState = R.drawable.appwidget_button_controlling_on;
                } else if (isArmed) {
                    // Armed towards On
                    resourceForState = R.drawable.appwidget_button_armed_off;
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

        int resourceForState = getDeviceResourceForState(mIsReachable, mIsOn, mIsArmed, mIsControlling);
        views.setInt(R.id.control_button, "setBackgroundResource", resourceForState);
        appWidgetManager.updateAppWidget(mAppWidgetId, views);
    }

    private void startControl() {
        mIsControlling = true;
        displayDeviceState();
        ShowMessage("Sending control command..");
    }

    private void endControl(boolean success, String reason) {
        mIsControlling = false;
        displayDeviceState();
        if (success) {
            ShowMessage("Control OK");
        } else {
            ShowMessage("Control failed: " + reason);
        }
    }

    private void endControl(boolean success, String reason, String details) {
        if (details != null)
            Log.e("WidgetEndControl", details);
        endControl(success, reason);
    }

    private void updateDeviceState() {
        String deviceId = PersistentStorage.getInstance().loadDeviceId(this, mAppWidgetId);
        if (deviceId == null) {
            ShowMessage("Configuration issue. Stored device not found (null). Please remove and recreate the device widget");
            return;
        }

        stateMgr.updateCurrentState(deviceId, new CozifyAPI.CozifyCallback() {
            @Override
            public void result(boolean success, String status, JSONObject resultJson, JSONObject requestJson) {
                if (success) {
                    CozifySceneOrDeviceState state = stateMgr.getCurrentState();
                    mIsOn = state.isOn;
                    mIsReachable = state.reachable;
                    displayDeviceState();
                } else {
                    mIsReachable = false;
                }
            }
        });
    }

    private boolean toggelOnOff() {
        startControl();
        String deviceId = PersistentStorage.getInstance().loadDeviceId(this, mAppWidgetId);
        if (deviceId == null) {
            endControl(false, "Configuration issue. Stored device not found (null). Please remove and recreate the device widget");
            return false;
        }
        controlToggle(deviceId);
        return true;
    }

    void controlToggle(final String id) {
        stateMgr = new CozifySceneOrDeviceStateManager();
        stateMgr.toggleState(id, new CozifyAPI.CozifyCallback() {
            @Override
            public void result(boolean success, String status, JSONObject jsonResponse, JSONObject jsonRequest) {
                if (success) {
                    mIsOn = stateMgr.getCurrentState().isOn;
                    mIsReachable = stateMgr.getCurrentState().reachable;
                    endControl(true, status);
                } else {
                    mIsArmed = false;
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

