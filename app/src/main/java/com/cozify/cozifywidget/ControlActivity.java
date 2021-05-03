package com.cozify.cozifywidget;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;

public class ControlActivity extends AppCompatActivity {
    private TextView textViewStatus;

    private ControlState controlState;
    private SettingsWidget settingsWidget;

    private static Handler handler = new Handler();
    private Runnable delayedDisarm = null;

    private CozifySceneOrDeviceStateManager stateMgr;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        int appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;

        // Set the result to CANCELED.  This will cause the widget host to cancel
        // out of the widget placement if they press the back button.
        setResult(RESULT_CANCELED);
        setContentView(R.layout.activity_control);
        initViews();
        appWidgetId = initWidgetId();
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            ShowMessage(appWidgetId, "WidgetId not found. Reinstall this widget.");
            finish();
            return;
        }
        Log.d("CLICK", "WidgetID:" + appWidgetId);
        if (stateMgr == null) {
            stateMgr = new CozifySceneOrDeviceStateManager(this, appWidgetId);
        }

        Intent resultValue = new Intent();
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        setResult(RESULT_CANCELED, resultValue);

        if (!loadSavedSettings(appWidgetId)) {
            updateDeviceState(appWidgetId, "onCreate()");
        } else {
            if (handleClick(appWidgetId)) {
                setResult(RESULT_OK, resultValue);
            }
        }
        updateAllOtherWidgets(appWidgetId);
        finish();
    }

    private boolean handleToggleClick(int appWidgetId) {
        if (!controlState.shouldUpdate() &&
                (controlState.isControlling() || controlState.isArming() || controlState.isUpdating())) {
            return true;
        } else {
            if (!settingsWidget.getSafeControl()) {
                updateDeviceStateAndToggle(appWidgetId);
            } else {
                if (isArmed()) {
                    if (toggleOnOff(appWidgetId)) {
                        return true;
                    } else {
                        updateDeviceState(appWidgetId, "handleToggleClick()");
                    }
                } else {
                    updateDeviceStateAndArm(appWidgetId);
                    return true;
                }
            }
        }
        return false;
    }

    private boolean handleMeasurementUpdate(int appWidgetId) {
        updateDeviceState(appWidgetId, "handleMeasurementUpdate()");
        return true;
    }

    private boolean handleClick(int appWidgetId) {
        CozifySceneOrDeviceState state = stateMgr.getCurrentState();
        if (state != null) {
            if (!state.initialized) {
                updateDeviceState(appWidgetId, "handleClick().!state.initialized");
            }
            if (state.isOnOff()) {
                return handleToggleClick(appWidgetId);
            }
            if (state.isSensor()) {
                return handleMeasurementUpdate(appWidgetId);
            }
        } else {
            updateDeviceState(appWidgetId, "handleClick().state==null");
        }
        return false;
    }

    static int[] addElement(int[] a, int e) {
        a = Arrays.copyOf(a, a.length + 1);
        a[a.length - 1] = e;
        return a;
    }

    private void updateAllOtherWidgets(int appWidgetId) {
        Intent intent = new Intent(this, CozifyAppWidget.class);
        intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        int[] ids = AppWidgetManager.getInstance(getApplication()).getAppWidgetIds(new ComponentName(getApplication(), CozifyAppWidget.class));
        int[] other_ids = new int[0];
        for (int id : ids) {
            if (id != appWidgetId) {
                other_ids = addElement(other_ids, id);
            }
        }
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, other_ids);
        sendBroadcast(intent);
        updateAllOtherWidgetsDouble(appWidgetId);
    }

    private void updateAllOtherWidgetsDouble(int appWidgetId) {
        Intent intent = new Intent(this, CozifyAppWidgetDouble.class);
        intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        int[] ids = AppWidgetManager.getInstance(getApplication()).getAppWidgetIds(new ComponentName(getApplication(), CozifyAppWidgetDouble.class));
        int[] other_ids = new int[0];
        for (int id : ids) {
            if (id != appWidgetId) {
                other_ids = addElement(other_ids, id);
            }
        }
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, other_ids);
        sendBroadcast(intent);
    }


    private void ShowMessage(int appWidgetId, String message) {
        setStatusMessage(message);
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        Log.i("Widget:" + appWidgetId, message);
    }

    private void ShowErrorMessage(int appWidgetId, String errorMessage, String details) {
        ShowMessage(appWidgetId, errorMessage);
        Log.e("Widget: " + appWidgetId + " ERROR details:", details);
    }

    private boolean isArmed() {
        return controlState.isArmed();
    }

    private void arm(final int appWidgetId, boolean armForOnOff) {
        controlState.setArmed(true);
        controlState.setArmedForDesiredState(armForOnOff);
        saveState();
        ShowMessage(appWidgetId, "Press again in 5s to control " + (controlState.getArmedForDesiredState() ? "ON" : "OFF"));
        displayDeviceState(appWidgetId, "arm");
        delayedDisarm = new Runnable() {
            @Override
            public void run() {
                loadSavedSettings(appWidgetId);
                if (controlState.isArmed()) {
                    controlState.setArmed(false);
                    saveState();
                    displayDeviceState(appWidgetId, "delayedDisarm");
                }
            }
        };
        handler.postDelayed(delayedDisarm, 5000);
    }

    private void cancelDelayedDisarm() {
        handler.removeCallbacksAndMessages(null);
    }

    private void disarm(int appWidgetId) {
        cancelDelayedDisarm();
        if (delayedDisarm != null) {
            handler.removeCallbacks(delayedDisarm);
            delayedDisarm = null;
        }
        controlState.setArmed(false);
        saveState();
        displayDeviceState(appWidgetId, "disarm");
    }

    private void saveState() {
        stateMgr.saveState();
    }

    private boolean loadSavedSettings(int appWidgetId) {
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            ShowMessage(appWidgetId, "INVALID_APPWIDGET_ID : Loading settingsHub failed!");
            return false;
        }
        final Context context = ControlActivity.this;

        controlState = new ControlState(context, appWidgetId);
        if (!controlState.init) {
            ShowErrorMessage(appWidgetId, "FAILED to load controlState", "PersistentStorage returned null.");
            return false;
        }

        settingsWidget = new SettingsWidget(context, appWidgetId);
        if (!settingsWidget.init || settingsWidget.getDeviceId() == null) {
            ShowErrorMessage(appWidgetId, "Configuration issue", "Stored device ID not found (null). Please remove and recreate the device widget");
            return false;
        }
        return true;
    }

    private int initWidgetId() {
        int appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
        Intent intent = this.getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            appWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        }
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        }

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            ShowMessage(appWidgetId, "ERROR: appWidgetId not found in extra (INVALID_APPWIDGET_ID)");
            finish();
            return AppWidgetManager.INVALID_APPWIDGET_ID;
        }
        return appWidgetId;
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

    private void displayDeviceState(int appWidgetId, String why) {
        final Context context = ControlActivity.this;
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
        int resourceForState = updateDeviceState(settingsWidget,
                controlState,
                stateMgr, appWidgetId, appWidgetManager, this.getPackageName());
        int layout = R.layout.appwidget_button;
        AppWidgetProviderInfo pi = appWidgetManager.getAppWidgetInfo(appWidgetId);
        if (pi != null) {
            layout = pi.initialLayout;
        }
        RemoteViews views = new RemoteViews(this.getPackageName(),
                layout);
        int bid = R.id.control_button;
        if (layout == R.layout.appwidget_button_double) {
            bid = R.id.control_button_double;
        }
        views.setContentDescription(bid, context.getResources().getResourceEntryName(resourceForState));
        appWidgetManager.updateAppWidget(appWidgetId, views);
        Log.d("ResourceForState", String.format("%s : %s (%d)", why, getResources().getResourceEntryName(resourceForState), resourceForState));
    }

    public static boolean hasArrayString(JSONArray array, String str) {
        try {
            for (int i = 0; i<array.length();i++) {
                if (array.getString(i).equals(str)) {
                    return true;
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static int updateDeviceState(SettingsWidget settingsWidget, ControlState controlState,
                                        CozifySceneOrDeviceStateManager stateMgr,
                                        int appWidgetId, AppWidgetManager appWidgetManager, String packageName) {

        String device_name = settingsWidget.getDeviceName();
        JSONArray capabilities = settingsWidget.getSelectedCapabilities();
        String measurement = stateMgr.getMeasurementString(capabilities);
        String measurementTimeStr = stateMgr.getMeasurementTimeStr();
        CozifySceneOrDeviceState s = stateMgr.getCurrentState();
        boolean isSensor = false;
        boolean isControllable = false;
        if (s != null) {
            isSensor = s.isSensor() && !s.isOnOff();
            isControllable = hasArrayString(capabilities, "ON_OFF");
        }

        int layout = R.layout.appwidget_button;
        AppWidgetProviderInfo pi = appWidgetManager.getAppWidgetInfo(appWidgetId);
        if (pi != null) {
            layout = pi.initialLayout;
        }

        int resourceForState = getDeviceResourceForState(stateMgr.isReachable(), stateMgr.isOn(),
                controlState.isArmed(), controlState.isArming(), controlState.isControlling(), isSensor, controlState.isUpdating());

        RemoteViews views = new RemoteViews(packageName, layout);
        if (layout == R.layout.appwidget_button_double) {
            UpdateLayoutDouble(views, isControllable, resourceForState, device_name, measurement, measurementTimeStr, controlState, settingsWidget);
        } else {
            UpdateLayout(views, isControllable,resourceForState, device_name, measurement, controlState, settingsWidget);
        }

        appWidgetManager.updateAppWidget(appWidgetId, views);
        return resourceForState;
    }

    private static void UpdateLayout(RemoteViews views, boolean isControllable, int resourceForState, String device_name, String measurement, ControlState controlState, SettingsWidget settingsWidget) {
        int bid = R.id.control_button;
        views.setInt(bid, "setBackgroundResource", resourceForState);
        views.setBoolean(bid, "setEnabled", !(controlState.isControlling() || controlState.isArming() || controlState.isUpdating()));
        if (measurement != null) {
            String label = measurement;
            if (device_name != null) {
                label = measurement + "\n" + device_name;
            }
            views.setCharSequence(bid, "setText", label);
            views.setFloat(bid, "setTextSize", settingsWidget.getTextSize());
        } else {
            if (device_name != null) {
                views.setCharSequence(bid, "setText", device_name);
            }
        }
    }

    private static void UpdateLayoutDouble(RemoteViews views, boolean isControllable, int resourceForState, String device_name, String measurement, String age, ControlState controlState, SettingsWidget settingsWidget) {
        int bid = R.id.control_button_double;
        if (isControllable) {
            views.setInt(bid, "setVisibility", View.VISIBLE);
        } else {
            views.setInt(bid, "setVisibility", View.GONE);
        }
        views.setInt(bid, "setBackgroundResource", resourceForState);
        views.setBoolean(bid, "setEnabled", !(controlState.isControlling() || controlState.isArming() || controlState.isUpdating()));
        views.setCharSequence(bid, "setText", "");

        if (device_name != null) {
            views.setCharSequence(R.id.control_button_device_name, "setText", device_name);
        }
        if (measurement != null) {
            views.setCharSequence(R.id.control_button_measurement, "setText", measurement);
            views.setCharSequence(R.id.widget_button_updated_since, "setText", age);
        } else {
            views.setCharSequence(R.id.control_button_measurement, "setText", "");
            views.setCharSequence(R.id.widget_button_updated_since, "setText", "");
        }
    }

    private void startControl(int appWidgetId) {
        controlState.setControlling(true);
        saveState();
        displayDeviceState(appWidgetId, "startControl");
        ShowMessage(appWidgetId, String.format("Controlling %s to %s", settingsWidget.getDeviceName(),
                controlState.getArmedForDesiredState()? "ON" : "OFF"));
    }

    private void endControl(final int appWidgetId, boolean success, String reason) {
        controlState.setControlling(false);
        saveState();
        displayDeviceState(appWidgetId, "endControl");
        if (success) {
            ShowMessage(appWidgetId, String.format("Control %s OK", settingsWidget.getDeviceName()));
        } else {
            ShowErrorMessage(appWidgetId, String.format("FAILED to control %s", settingsWidget.getDeviceName()), reason);
        }
    }

    private void endControl(int appWidgetId, boolean success, String reason, String details) {
        if (details != null)
            Log.e("WidgetEndControl", details);
        endControl(appWidgetId, success, reason);
    }

    private void updateDeviceState(final int appWidgetId, String why) {
        if (controlState.isUpdating()) return;
        controlState.setUpdating(true);
        saveState();
        displayDeviceState(appWidgetId, why + ".updateDeviceState()");
        if (settingsWidget.getDeviceId() == null) {
            ShowMessage(appWidgetId, "Configuration issue. Stored device not found (null). Please remove and recreate the device widget");
            return;
        }

        stateMgr.updateCurrentState(settingsWidget.getDeviceId(), false, new CozifyApiReal.CozifyCallback() {
            @Override
            public void result(boolean success, String status, JSONObject resultJson, JSONObject requestJson) {
                controlState.setUpdating(false);
                saveState();
                displayDeviceState(appWidgetId, "updateDeviceState().stateMgr.updateCurrentState");
            }
        });
    }

    private void loginAgain() {
        Intent intent = new Intent(this, CozifyWidgetSetupActivity.class);
        startActivity(intent);
    }

    private void updateDeviceStateAndArm(final int appWidgetId) {
        if (settingsWidget.getDeviceId() == null) {
            ShowMessage(appWidgetId, "Configuration issue. Stored device not found (null). Please remove and recreate the device widget");
            return;
        }
        controlState.setArming(true);
        saveState();
        displayDeviceState(appWidgetId, "updateDeviceStateAndArm");
        stateMgr.updateCurrentState(settingsWidget.getDeviceId(), true, new CozifyApiReal.CozifyCallback() {
            @Override
            public void result(boolean success, String status, JSONObject resultJson, JSONObject requestJson) {
                if (success) {
                    loadSavedSettings(appWidgetId);
                    CozifySceneOrDeviceState state = stateMgr.getCurrentState();
                    arm(appWidgetId, !state.isOn);
                } else {
                    stateMgr.setReachable(false);
                    controlState.setArming(false);
                    saveState();
                    displayDeviceState(appWidgetId, "updateDeviceStateAndArm().stateMgr.updateCurrentState");
                }
            }
        });
    }

    private void updateDeviceStateAndToggle(final int appWidgetId) {
        if (settingsWidget.getDeviceId() == null) {
            ShowMessage(appWidgetId, "Configuration issue. Stored device not found (null). Please remove and recreate the device widget");
            return;
        }
        controlState.setArming(true);
        saveState();
        displayDeviceState(appWidgetId, "updateDeviceStateAndToggle");
        stateMgr.updateCurrentState(settingsWidget.getDeviceId(), true, new CozifyApiReal.CozifyCallback() {
            @Override
            public void result(boolean success, String status, JSONObject resultJson, JSONObject requestJson) {
                if (success) {
                    loadSavedSettings(appWidgetId);
                    CozifySceneOrDeviceState state = stateMgr.getCurrentState();
                    controlState.setArmedForDesiredState(!state.isOn);
                    saveState();
                    toggleOnOff(appWidgetId);
                } else {
                    stateMgr.setReachable(false);
                    controlState.setArming(false);
                    saveState();
                    displayDeviceState(appWidgetId, "updateDeviceStateAndToggle().stateMgr.updateCurrentState");
                }
            }
        });
    }

    private boolean toggleOnOff(int appWidgetId) {
        startControl(appWidgetId);
        if (settingsWidget.getDeviceId() == null) {
            endControl(appWidgetId, false, "Configuration issue. Stored device not found (null). Please remove and recreate the device widget");
            return false;
        }
        controlToggle(appWidgetId);
        return true;
    }

    void controlToggle(final int appWidgetId) {
        stateMgr.controlStateToDesired(settingsWidget.getDeviceId(), controlState.getArmedForDesiredState(), new CozifyApiReal.CozifyCallback() {
            @Override
            public void result(boolean success, String status, JSONObject jsonResponse, JSONObject jsonRequest) {
                if (success) {
                    endControl(appWidgetId, true, status);
                } else {
                    stateMgr.setReachable(false);
                    String details = "Details:";
                    if (jsonResponse != null)
                        details += " jsonResponse: "+jsonResponse.toString();
                    if (jsonRequest != null)
                        details += " jsonRequest: "+jsonRequest.toString();
                    endControl(appWidgetId, false, status, details);
                }
            }
        });
    }
}

