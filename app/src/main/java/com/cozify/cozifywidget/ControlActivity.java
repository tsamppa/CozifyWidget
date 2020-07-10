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
    private int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
    private TextView textViewStatus;

    private ControlState controlState;
    private WidgetSettings widgetSettings;

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
        Log.d("CLICK", "WidgetID:" + mAppWidgetId);
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
        updateAllOtherWidgets();
        finish();
    }

    private boolean handleToggleClick() {
        if (!controlState.shouldUpdate() &&
                (controlState.isControlling() || controlState.isArming() || controlState.isUpdating())) {
            return true;
        } else {
            if (isArmed()) {
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
            if (state.isSensor()) {
                return handleMeasurementUpdate();
            }
        } else {
            updateDeviceState("handleClick().state==null");
        }
        return false;
    }

    static int[] addElement(int[] a, int e) {
        a = Arrays.copyOf(a, a.length + 1);
        a[a.length - 1] = e;
        return a;
    }

    private void updateAllOtherWidgets() {
        Intent intent = new Intent(this, CozifyAppWidget.class);
        intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        int[] ids = AppWidgetManager.getInstance(getApplication()).getAppWidgetIds(new ComponentName(getApplication(), CozifyAppWidget.class));
        int[] other_ids = new int[0];
        for (int id : ids) {
            if (id != mAppWidgetId) {
                other_ids = addElement(other_ids, id);
            }
        }
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, other_ids);
        sendBroadcast(intent);
        updateAllOtherWidgetsDouble();
    }

    private void updateAllOtherWidgetsDouble() {
        Intent intent = new Intent(this, CozifyAppWidgetDouble.class);
        intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        int[] ids = AppWidgetManager.getInstance(getApplication()).getAppWidgetIds(new ComponentName(getApplication(), CozifyAppWidgetDouble.class));
        int[] other_ids = new int[0];
        for (int id : ids) {
            if (id != mAppWidgetId) {
                other_ids = addElement(other_ids, id);
            }
        }
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, other_ids);
        sendBroadcast(intent);
    }


    private void ShowMessage(String message) {
        setStatusMessage(message);
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        Log.i("Widget:" + mAppWidgetId, message);
    }

    private void ShowErrorMessage(String errorMessage, String details) {
        ShowMessage(errorMessage);
        Log.e("Widget: " + mAppWidgetId + " ERROR details:", details);
    }

    private boolean isArmed() {
        return controlState.isArmed();
    }

    private void arm(boolean armForOnOff) {
        controlState.setArmed(true);
        controlState.setArmedForDesiredState(armForOnOff);
        saveState();
        ShowMessage("Press again in 5s to control " + (controlState.getArmedForDesiredState() ? "ON" : "OFF"));
        displayDeviceState("arm");
        delayedDisarm = new Runnable() {
            @Override
            public void run() {
                loadSavedSettings();
                if (controlState.isArmed()) {
                    controlState.setArmed(false);
                    saveState();
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
        controlState.setArmed(false);
        saveState();
        displayDeviceState("disarm");
    }

    private void saveState() {
        stateMgr.saveState();
    }

    private boolean loadSavedSettings() {
        if (mAppWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            ShowMessage("INVALID_APPWIDGET_ID : Loading settingsHub failed!");
            return false;
        }
        final Context context = ControlActivity.this;

        controlState = new ControlState(context, mAppWidgetId);
        if (!controlState.init) {
            ShowErrorMessage("FAILED to load settingsHub", "PersistentStorage returned null.");
            return false;
        }

        widgetSettings = new WidgetSettings(context, mAppWidgetId);
        if (!widgetSettings.init || widgetSettings.getDeviceId() == null) {
            ShowErrorMessage("Configuration issue", "Stored device ID not found (null). Please remove and recreate the device widget");
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
        int resourceForState = updateDeviceState(widgetSettings,
                controlState,
                stateMgr, mAppWidgetId, appWidgetManager, this.getPackageName());
        int layout = R.layout.appwidget_button;
        AppWidgetProviderInfo pi = appWidgetManager.getAppWidgetInfo(mAppWidgetId);
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
        appWidgetManager.updateAppWidget(mAppWidgetId, views);
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

    public static int updateDeviceState(WidgetSettings widgetSettings, ControlState controlState,
                                        CozifySceneOrDeviceStateManager stateMgr,
                                        int appWidgetId, AppWidgetManager appWidgetManager, String packageName) {

        String device_name = widgetSettings.getDeviceName();
        JSONArray capabilities = widgetSettings.getSelectedCapabilities();
        String measurement = stateMgr.getMeasurementString(capabilities);
        String age = stateMgr.getMeasurementAge();
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
            UpdateLayoutDouble(views, isControllable, resourceForState, device_name, measurement, age, controlState, widgetSettings);
        } else {
            UpdateLayout(views, isControllable,resourceForState, device_name, measurement, controlState, widgetSettings);
        }

        appWidgetManager.updateAppWidget(appWidgetId, views);
        return resourceForState;
    }

    private static void UpdateLayout(RemoteViews views, boolean isControllable, int resourceForState, String device_name, String measurement, ControlState controlState, WidgetSettings widgetSettings) {
        int bid = R.id.control_button;
        views.setInt(bid, "setBackgroundResource", resourceForState);
        views.setBoolean(bid, "setEnabled", !(controlState.isControlling() || controlState.isArming() || controlState.isUpdating()));
        if (measurement != null) {
            String label = measurement;
            if (device_name != null) {
                label = measurement + "\n" + device_name;
            }
            views.setCharSequence(bid, "setText", label);
            views.setFloat(bid, "setTextSize", widgetSettings.getTextSize());
        } else {
            if (device_name != null) {
                views.setCharSequence(bid, "setText", device_name);
            }
        }
    }

    private static void UpdateLayoutDouble(RemoteViews views, boolean isControllable, int resourceForState, String device_name, String measurement, String age, ControlState controlState, WidgetSettings widgetSettings) {
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
            views.setInt(R.id.widget_button_refresh, "setVisibility", View.VISIBLE);
            views.setCharSequence(R.id.control_button_measurement, "setText", measurement);
            views.setCharSequence(R.id.widget_button_updated_since, "setText", age);
        } else {
            views.setInt(R.id.widget_button_refresh, "setVisibility", View.GONE);
            views.setCharSequence(R.id.control_button_measurement, "setText", "");
            views.setCharSequence(R.id.widget_button_updated_since, "setText", "");
        }
    }

    private void startControl() {
        controlState.setControlling(true);
        saveState();
        displayDeviceState("startControl");
        ShowMessage(String.format("Controlling %s to %s", widgetSettings.getDeviceName(),
                controlState.getArmedForDesiredState()? "ON" : "OFF"));
    }

    private void endControl(boolean success, String reason) {
        controlState.setControlling(false);
        saveState();
        displayDeviceState("endControl");
        if (success) {
            ShowMessage(String.format("Control %s OK", widgetSettings.getDeviceName()));
        } else {
            ShowErrorMessage(String.format("FAILED to control %s", widgetSettings.getDeviceName()), reason);
        }
    }

    private void endControl(boolean success, String reason, String details) {
        if (details != null)
            Log.e("WidgetEndControl", details);
        endControl(success, reason);
    }

    private void updateDeviceState(String why) {
        if (controlState.isUpdating()) return;
        controlState.setUpdating(true);
        saveState();
        displayDeviceState(why + ".updateDeviceState()");
        if (widgetSettings.getDeviceId() == null) {
            ShowMessage("Configuration issue. Stored device not found (null). Please remove and recreate the device widget");
            return;
        }

        stateMgr.updateCurrentState(widgetSettings.getDeviceId(), true, new CozifyApiReal.CozifyCallback() {
            @Override
            public void result(boolean success, String status, JSONObject resultJson, JSONObject requestJson) {
                controlState.setUpdating(false);
                saveState();
                displayDeviceState("updateDeviceState().stateMgr.updateCurrentState");
            }
        });
    }

    private void loginAgain() {
        Intent intent = new Intent(this, CozifyWidgetSetupActivity.class);
        startActivity(intent);
    }

    private void updateDeviceStateAndArm() {
        if (widgetSettings.getDeviceId() == null) {
            ShowMessage("Configuration issue. Stored device not found (null). Please remove and recreate the device widget");
            return;
        }
        controlState.setArming(true);
        saveState();
        displayDeviceState("updateDeviceStateAndArm");
        stateMgr.updateCurrentState(widgetSettings.getDeviceId(), true, new CozifyApiReal.CozifyCallback() {
            @Override
            public void result(boolean success, String status, JSONObject resultJson, JSONObject requestJson) {
                if (success) {
                    loadSavedSettings();
                    CozifySceneOrDeviceState state = stateMgr.getCurrentState();
                    arm(!state.isOn);
                } else {
                    stateMgr.setReachable(false);
                    controlState.setArming(false);
                    saveState();
                    displayDeviceState("updateDeviceStateAndArm().stateMgr.updateCurrentState");
                }
            }
        });
    }


    private boolean toggleOnOff() {
        startControl();
        if (widgetSettings.getDeviceId() == null) {
            endControl(false, "Configuration issue. Stored device not found (null). Please remove and recreate the device widget");
            return false;
        }
        controlToggle();
        return true;
    }

    void controlToggle() {
        stateMgr.controlStateToDesired(widgetSettings.getDeviceId(), controlState.getArmedForDesiredState(), new CozifyApiReal.CozifyCallback() {
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

