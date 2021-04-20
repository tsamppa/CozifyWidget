package com.cozify.cozifywidget;

import android.app.Activity;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RemoteViews;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.textfield.TextInputLayout;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.Locale;

public class CozifyAppWidgetConfigure extends Activity {
    public static final String SHARED_PRES = "com.cozify.android.apis.appwidget.CozifyWidgetProvider";
    public static final String KEY_BUTTON_TEXT = "Control device";
    private int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;

    private CozifyApiReal cozifyAPI = new CozifyApiReal(this);

    EditText editTextDeviceName;
    TextInputLayout textInputLayoutDeviceName;
    TextView textViewStatus;
    Button buttonCreate;
    String selectedDeviceName;
    String selectedDeviceShortName;
    String selectedDeviceId;
    String selectedHubKey;
    String selectedHubName;
    String hubLanIp;
    String hubApiVersion;
    float selectedTextSize = 17;
    boolean connected = false;
    boolean spinnersEnabled = false;
    JSONObject hubkeysJson;
    JSONObject hubNamesJson;
    JSONObject hubIdsJson;
    JSONObject devicesJson;
    ArrayList<String> devicesList;

    boolean selectedDeviceCapability_on_off = false;
    boolean selectedSafeControl = false;
    boolean selectedDeviceCapability_temperature = false;
    boolean selectedDeviceCapability_co2 = false;
    boolean selectedDeviceCapability_humidity = false;
    boolean selectedDeviceCapability_lux = false;
    boolean selectedDeviceCapability_watt = false;

    public CozifyAppWidgetConfigure() {
        super();
    }

    @Override
    public void onCreate(Bundle savedInstanceSate) {
        super.onCreate(savedInstanceSate);
        final Context context = CozifyAppWidgetConfigure.this;
        // Set the result to CANCELED.  This will cause the widget host to cancel
        // out of the widget placement if they press the back button.
        setResult(RESULT_CANCELED);
        // Set the view layout resource to use.
        setContentView(R.layout.appwidget_configure);

        Intent configIntent = getIntent();
        Bundle extras = configIntent.getExtras();
        if (extras != null) {
            mAppWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        }
        if (mAppWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            mAppWidgetId = configIntent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        }
        Intent resultValue = new Intent();
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
        setResult(RESULT_CANCELED, resultValue);

        if (mAppWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            ShowErrorMessage("Invalid Widget ID", "mAppWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID");
            finish();
        }
        cozifyAPI.loadState(mAppWidgetId);

        devicesList = new ArrayList<>();

        PackageInfo pInfo = null;
        try {
            pInfo = context.getPackageManager().getPackageInfo(getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        if (pInfo != null) {
            TextView textViewVersion = findViewById(R.id.version);
            AppWidgetProviderInfo pi = AppWidgetManager.getInstance(this).getAppWidgetInfo(mAppWidgetId);
            int layout = R.layout.appwidget_button;
            if (pi != null) {
                layout = pi.initialLayout;
            }
            String size = " Creating Single Size Widget";
            if (layout == R.layout.appwidget_button_double) {
                size = " Creating Double Size Widget";
            }
            textViewVersion.setText(String.format(Locale.ENGLISH, "Cozify Widgets Version %s (%d) - %s",
                    pInfo.versionName, pInfo.versionCode, size));
        }
        buttonCreate = findViewById(R.id.create_button);
        buttonCreate.setEnabled(false);
        if (cozifyAPI.getCloudToken() == null) {
            setStatus("Error in setting cannot connect to cloud. Please re-login.");
            Intent intent = new Intent(this, CozifyWidgetSetupActivity.class);
            startActivity(intent);
            return;
        }
        // Find the Device Name EditText
        editTextDeviceName = findViewById(R.id.device_name_edit);
        textInputLayoutDeviceName = findViewById(R.id.control_button_device_name);
        editTextDeviceName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                int str_len = s.length();
                if (str_len > 0) {
                    textInputLayoutDeviceName.setErrorEnabled(false);
                } else {
                    textInputLayoutDeviceName.setErrorEnabled(true);
                    textInputLayoutDeviceName.setError("Please enter a shorthand name for the device");
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        findViewById(R.id.test_control_on_button).setOnClickListener(mOnClickListenerTestOn);
        findViewById(R.id.test_control_off_button).setOnClickListener(mOnClickListenerTestOff);

        Switch switch_on_off = (Switch) findViewById(R.id.switch_device_capability_on_off);
        switch_on_off.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                selectedDeviceCapability_on_off = isChecked;
            }
        });

        Switch switch_safety = (Switch) findViewById(R.id.switch_safe_control);
        switch_safety.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                selectedSafeControl = isChecked;
            }
        });


        Switch switch_temp = (Switch) findViewById(R.id.switch_device_capability_temperature);
        switch_temp.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                selectedDeviceCapability_temperature = isChecked;
            }
        });

        Switch switch_hum = (Switch) findViewById(R.id.switch_device_capability_humidity);
        switch_hum.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                selectedDeviceCapability_humidity = isChecked;
            }
        });

        Switch switch_co2 = (Switch) findViewById(R.id.switch_device_capability_co2);
        switch_co2.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                selectedDeviceCapability_co2 = isChecked;
            }
        });

        Switch switch_lux = (Switch) findViewById(R.id.switch_device_capability_lux);
        switch_lux.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                selectedDeviceCapability_lux = isChecked;
            }
        });

        Switch switch_watt = (Switch) findViewById(R.id.switch_device_capability_watt);
        switch_watt.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                selectedDeviceCapability_watt = isChecked;
            }
        });


        getHubKeys();

        Spinner hubsSpinner = findViewById(R.id.spinner_hubs);
        hubsSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                enableTestButtons(false);
                buttonCreate.setEnabled(false);
                devicesList = new ArrayList<>();

                Spinner spinner = findViewById(R.id.spinner_hubs);
                Object selectedItem = spinner.getSelectedItem();
                if (selectedItem != null) {
                    selectedHubName = selectedItem.toString();
                    try {
                        selectedHubKey = hubNamesJson.getString(selectedHubName);
                        String selectedHubId = hubIdsJson.getString(selectedHubName);
                        if (selectedHubKey != null) {
                            cozifyAPI.setApiVersion(null); // reset to refresh it
                            cozifyAPI.selectToUseHubWithKey(selectedHubKey, mAppWidgetId, new CozifyApiReal.CozifyCallback() {
                                @Override
                                public void result(boolean success, String status, JSONObject jsonResponse, JSONObject jsonRequest) {
                                    connected = success;
                                    if (success) {
                                        hubApiVersion = cozifyAPI.getApiVersion();
                                        getDevices();
                                    } else {
                                        String message = parseMessageFromJsonResponse(jsonResponse);
                                        setStatus(status + ": " + message);
                                    }
                                }
                            });
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                enableTestButtons(false);
                disableCapabilities();
            }

        });

        Spinner devicesSpinner = findViewById(R.id.spinner_devices);
        devicesSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                Spinner spinner = findViewById(R.id.spinner_devices);
                Object selectedItem = spinner.getSelectedItem();
                if (selectedItem != null) {
                    selectedDeviceName = selectedItem.toString();
                    selectedDeviceId = getDeviceIdForName(selectedDeviceName);
                    enableTestButtons(true);
                    enableCapabilities(selectedDeviceName);
                    buttonCreate.setEnabled(true);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                enableTestButtons(false);
                disableCapabilities();
            }
        });

    }

    void enableCapabilities(String selectedDeviceName) {
        boolean on_off = hasDeviceCapability(selectedDeviceName, "ON_OFF");
        if (selectedDeviceName.contains("Group:")) on_off = true;
        if (selectedDeviceName.contains("Scene:")) on_off = true;
        Switch s_onoff = (Switch) findViewById(R.id.switch_device_capability_on_off);
        s_onoff.setChecked(on_off);
        s_onoff.setEnabled(on_off);

        Switch s_safety = (Switch) findViewById(R.id.switch_safe_control);
        s_safety.setChecked(on_off);
        s_safety.setEnabled(on_off);

        boolean co2 = hasDeviceCapability(selectedDeviceName, "CO2");
        Switch s_co2 = (Switch) findViewById(R.id.switch_device_capability_co2);
        s_co2.setChecked(co2);
        s_co2.setEnabled(co2);

        boolean temp = hasDeviceCapability(selectedDeviceName, "TEMPERATURE");
        Switch s_temp = (Switch) findViewById(R.id.switch_device_capability_temperature);
        s_temp.setChecked(temp);
        s_temp.setEnabled(temp);

        boolean hum = hasDeviceCapability(selectedDeviceName, "HUMIDITY");
        Switch s_hum = (Switch) findViewById(R.id.switch_device_capability_humidity);
        s_hum.setChecked(hum);
        s_hum.setEnabled(hum);

        boolean lux = hasDeviceCapability(selectedDeviceName, "LUX");
        Switch s_lux = (Switch) findViewById(R.id.switch_device_capability_lux);
        s_lux.setChecked(lux);
        s_lux.setEnabled(lux);

        boolean watt = hasDeviceCapability(selectedDeviceName, "MEASURE_POWER");
        Switch s_watt = (Switch) findViewById(R.id.switch_device_capability_watt);
        s_watt.setChecked(watt);
        s_watt.setEnabled(watt);
    }

    void disableCapabilities() {
        findViewById(R.id.switch_device_capability_on_off).setSelected(false);
        findViewById(R.id.switch_device_capability_on_off).setEnabled(false);

        findViewById(R.id.switch_device_capability_co2).setSelected(false);
        findViewById(R.id.switch_device_capability_co2).setEnabled(false);

        findViewById(R.id.switch_device_capability_temperature).setSelected(false);
        findViewById(R.id.switch_device_capability_temperature).setEnabled(false);

        findViewById(R.id.switch_device_capability_humidity).setSelected(false);
        findViewById(R.id.switch_device_capability_humidity).setEnabled(false);

        findViewById(R.id.switch_device_capability_lux).setSelected(false);
        findViewById(R.id.switch_device_capability_lux).setEnabled(false);

        findViewById(R.id.switch_device_capability_watt).setSelected(false);
        findViewById(R.id.switch_device_capability_watt).setEnabled(false);

        findViewById(R.id.switch_safe_control).setSelected(false);
        findViewById(R.id.switch_safe_control).setEnabled(false);

    }

    JSONArray getSelectedCapabilities() {
        JSONArray caps = new JSONArray();
        if (selectedDeviceCapability_on_off)
            caps.put("ON_OFF");
        if (selectedDeviceCapability_co2)
            caps.put("CO2");
        if (selectedDeviceCapability_temperature)
            caps.put("TEMPERATURE");
        if (selectedDeviceCapability_humidity)
            caps.put("HUMIDITY");
        if (selectedDeviceCapability_lux)
            caps.put("LUX");
        if (selectedDeviceCapability_watt)
            caps.put("MEASURE_POWER");
        return caps;
    }

    void enableTestButtons(boolean enabled) {
        findViewById(R.id.test_control_on_button).setEnabled(enabled);
        findViewById(R.id.test_control_off_button).setEnabled(enabled);
    }

    void setStatus(String statusMessage) {
        if (textViewStatus == null) {
            textViewStatus = findViewById(R.id.config_status);
        }
        if (textViewStatus != null) {
            textViewStatus.setText(statusMessage);
        }
    }

    String getDeviceIdForName(String deviceName) {
        String id = "";
        try {
            id = devicesJson.getJSONObject(deviceName).getString("id");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return id;
    }

    JSONArray getDeviceCapabilitiesForName(String deviceName) {
        JSONArray caps = new JSONArray();
        try {
            caps = devicesJson.getJSONObject(deviceName).getJSONArray("capabilities");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return caps;
    }

    boolean hasArrayString(JSONArray array, String str) {
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

    boolean hasDeviceCapability(String deviceName, String capability) {
        JSONArray caps = getDeviceCapabilitiesForName(deviceName);
        boolean contains = hasArrayString(caps, capability);
        return contains;
    }


    private void revertToLocalHubConnection() {
        cozifyAPI.listHubs(new CozifyApiReal.StringCallback() {
            @Override
            public void result(boolean success, String message, String result) {
                if (success) {
                    hubLanIp = result.substring(2, result.length()-2);
                    cozifyAPI.setHubLanIpIfIdMatches(hubLanIp);
                    cozifyAPI.connectLocally();
                    getDevices();
                } else {
                    setStatus(message);
                }
            }
        });
    }

    public static PendingIntent createPendingIntentForWidgetClick(Context context, int appWidgetId) {

        Intent intent = new Intent(context.getApplicationContext(), ControlActivity.class);
        intent.setAction(Long.toString(System.currentTimeMillis()));
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        PendingIntent pendingIntent = PendingIntent.getActivity(context.getApplicationContext(), appWidgetId, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        return pendingIntent;
    }

    public static PendingIntent createPendingIntentForConfigClick(Context context, int appWidgetId) {
        Intent configIntent = new Intent(context, CozifyAppWidget.class)
                .setAction(CozifyAppWidget.ACTION_WIDGET_CONFIG_BUTTON_PRESSED);
        configIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        PendingIntent configPI = PendingIntent.getBroadcast(context, appWidgetId, configIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        return configPI;
    }

    private static int getTextSizeForWidth(int width) {
        return (width / 5)+1;
    }

    static public void setupWidgetButton(final Context context, final AppWidgetManager appWidgetManager,
                                  final int appWidgetId, final Bundle options, final WidgetSettings widgetSettings) {
        int layout = R.layout.appwidget_button;
        AppWidgetProviderInfo pi = appWidgetManager.getAppWidgetInfo(appWidgetId);
        if (pi != null) {
            layout = pi.initialLayout;
        }
        RemoteViews views = new RemoteViews(context.getPackageName(),
                layout);
        int bid = R.id.control_button;
        if (layout == R.layout.appwidget_button_double) {
            bid = R.id.control_button_double;
            int width = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH);
            int textSize = getTextSizeForWidth(width);
            if (Build.VERSION.SDK_INT < 25)
                views.setFloat(R.id.control_button_measurement, "setTextSize", textSize);
            if (textSize > 14 && widgetSettings.hasMeasurement()) {
                views.setInt(R.id.widget_button_refresh, "setVisibility", View.VISIBLE);
                views.setOnClickPendingIntent(R.id.widget_button_refresh, CozifyAppWidgetConfigure.createPendingIntentForWidgetClick(context, appWidgetId));
            } else {
                views.setInt(R.id.widget_button_refresh, "setVisibility", View.GONE);
            }
            views.setOnClickPendingIntent(R.id.widget_button_config, CozifyAppWidgetConfigure.createPendingIntentForConfigClick(context, appWidgetId));
            views.setOnClickPendingIntent(R.id.control_button_measurement, CozifyAppWidgetConfigure.createPendingIntentForWidgetClick(context, appWidgetId));
        }
        views.setOnClickPendingIntent(bid, CozifyAppWidgetConfigure.createPendingIntentForWidgetClick(context, appWidgetId));

        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    static void updateAppWidget(final Context context, final AppWidgetManager appWidgetManager,
                                final int appWidgetId, final Bundle options) {

        final WidgetSettings widgetSettings = new WidgetSettings(context, appWidgetId);
        if (widgetSettings.init && widgetSettings.getDeviceId() != null) {

            CozifyAppWidgetConfigure.setupWidgetButton(context, appWidgetManager, appWidgetId, options, widgetSettings);

            final ControlState controlState = new ControlState(context, appWidgetId);
            if (controlState.init) {
                final CozifySceneOrDeviceStateManager stateMgr = new CozifySceneOrDeviceStateManager(context, appWidgetId);
                if (controlState.shouldUpdate()) {
                    controlState.setUpdating(true);
                    updateIcon(context, appWidgetId, widgetSettings, controlState,
                            stateMgr, appWidgetManager);
                    stateMgr.updateCurrentState(widgetSettings.getDeviceId(), false, new CozifyApiReal.CozifyCallback() {
                        @Override
                        public void result(boolean success, String status, JSONObject jsonResponse, JSONObject jsonRequest) {
                            controlState.setUpdating(false);
                            updateIcon(context, appWidgetId, widgetSettings, controlState,
                                    stateMgr, appWidgetManager);
                        }
                    });
                } else {
                    updateIcon(context, appWidgetId, widgetSettings, controlState,
                            stateMgr, appWidgetManager);
                }
            }
        }
    }

    private static void updateIcon(Context context, int appWidgetId, WidgetSettings widgetSettings, ControlState controlState,
                                   CozifySceneOrDeviceStateManager stateMgr, AppWidgetManager appWidgetManager) {
        int resourceForState = ControlActivity.updateDeviceState(widgetSettings, controlState,
                stateMgr, appWidgetId, appWidgetManager, context.getPackageName());
        AppWidgetProviderInfo pi = appWidgetManager.getAppWidgetInfo(appWidgetId);
        int layout = R.layout.appwidget_button;
        if (pi != null) {
            layout = pi.initialLayout;
        }
        RemoteViews views = new RemoteViews(context.getPackageName(), layout);
        int bid = R.id.control_button;
        if (layout == R.layout.appwidget_button_double) {
            bid = R.id.control_button_double;
        }
        views.setContentDescription(bid, context.getResources().getResourceEntryName(resourceForState));
        Log.d("ResourceForState", String.format("%s : %s (%d)", "CozifyAppWidget.updateAppWidget", context.getResources().getResourceEntryName(resourceForState), resourceForState));
        // Tell the AppWidgetManager to perform an update on the current app widget
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    public void createWidget(View w) {
        final Context context = CozifyAppWidgetConfigure.this;

        String device_name = editTextDeviceName.getText().toString();

        if (selectedDeviceId != null && selectedDeviceId.length() > 0) {
            selectedDeviceShortName =  selectedDeviceName;
            if (device_name.length() > 0) {
                selectedDeviceShortName = device_name; // override name
            }

            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            // Save selected settingsHub
            WidgetSettings settings = new WidgetSettings(context, mAppWidgetId);
            settings.setDeviceId(selectedDeviceId);
            settings.setDeviceName(selectedDeviceShortName);
            settings.setTextSize(selectedTextSize);
            JSONArray selectedCaps = getSelectedCapabilities();
            settings.setSelectedCapabilities(selectedCaps);
            settings.setSafeControl(selectedSafeControl);
            ControlState controlState = new ControlState(context, mAppWidgetId);
            controlState.setControlling(false);

            Bundle options = appWidgetManager.getAppWidgetOptions(mAppWidgetId);
            Intent resultValue = new Intent();
            resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
            setResult(RESULT_OK, resultValue);
            String hubName = CozifyCloudToken.parseHubNameFromToken(selectedHubKey);
            ShowMessage("Widget "+mAppWidgetId+" created for controlling "+selectedDeviceShortName +" of HUB "+hubName);
            updateAppWidget(context, appWidgetManager, mAppWidgetId, options);
            updateAllWidgets();
            finish();
        } else {
            disableCapabilities();
            String error = "Please select device";
            ShowErrorMessage(error, "selectedDeviceId.length() <= 0 or null: ");
            if (textInputLayoutDeviceName != null) {
                textInputLayoutDeviceName.setErrorEnabled(true);
                textInputLayoutDeviceName.setError(error);
            }
        }
    }

    static int[] addElement(int[] a, int e) {
        a  = Arrays.copyOf(a, a.length + 1);
        a[a.length - 1] = e;
        return a;
    }

    private void updateAllWidgets() {
        Intent intent = new Intent(this, CozifyAppWidget.class);
        intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        int[] ids = AppWidgetManager.getInstance(getApplication()).getAppWidgetIds(new ComponentName(getApplication(), CozifyAppWidget.class));
        int[] ids2 = AppWidgetManager.getInstance(getApplication()).getAppWidgetIds(new ComponentName(this.getPackageName(), CozifyAppWidgetDouble.class.getName()));
        for (int id: ids2) {
            ids = addElement(ids, id);
        }
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
        sendBroadcast(intent);
    }

    private void updateCurrentState(String device_id) {
        cozifyAPI.getSceneOrDeviceState(device_id, true, new CozifyApiReal.CozifyCallback() {
            public void result(boolean success, String status, JSONObject jsonResponse, JSONObject jsonRequest) {
                if (success) {
                    CozifySceneOrDeviceState state = new CozifySceneOrDeviceState();
                    state.fromJson(jsonResponse);
                    PersistentStorage.getInstance(CozifyAppWidgetConfigure.this).saveDeviceState(mAppWidgetId, state);
                }
            }
        });
    }


    private void ShowMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        Log.i("Widget:"+mAppWidgetId, message);
    }

    private void ShowErrorMessage(String errorMessage, String details) {
        ShowMessage(errorMessage);
        Log.e("Widget: "+mAppWidgetId+" ERROR details:", details);
    }

    View.OnClickListener mOnClickListenerTestOn = new View.OnClickListener() {
        public void onClick(View v) {
            if (selectedDeviceId != null && selectedDeviceId.length() > 0) {
                int type = 1;
                if (selectedDeviceName.contains("Scene:")) type = 2;
                if (selectedDeviceName.contains("Group:")) type = 3;
                cozifyAPI.controlOnOff(selectedDeviceId, type, true, new CozifyApiReal.CozifyCallback(){
                    @Override
                    public void result(boolean success, String status, JSONObject jsonResponse, JSONObject jsonRequest) {
                        setStatus("Test result:" + status);
                    }
                });
            } else {
                setStatus(getString(R.string.tip_select_device_first));
            }
        }
    };

    View.OnClickListener mOnClickListenerTestOff = new View.OnClickListener() {
        public void onClick(View v) {
            if (selectedDeviceId != null && selectedDeviceId.length() > 0 && selectedDeviceName != null) {
                int type = 1;
                if (selectedDeviceName.contains("Scene:")) type = 2;
                if (selectedDeviceName.contains("Group:")) type = 3;

                cozifyAPI.controlOnOff(selectedDeviceId, type, false, new CozifyApiReal.CozifyCallback(){
                    @Override
                    public void result(boolean success, String status, JSONObject jsonResponse, JSONObject jsonRequest) {
                        setStatus("Test result:" + status);
                    }
                });
            } else {
                setStatus(getString(R.string.tip_select_device_first));
            }
        }
    };

    void enableSpinners(boolean enable) {
        spinnersEnabled = enable;
        Spinner ds = findViewById(R.id.spinner_devices);
        ds.setEnabled(enable);
        Spinner hs = findViewById(R.id.spinner_hubs);
        hs.setEnabled(enable);
    }

    private void getDevices() {
        setStatus("Wait while fetching list of devices..");
        enableSpinners(false);
        disableCapabilities();
        String[] capabilities = {"ON_OFF", "TEMPERATURE", "HUMIDITY", "CO2"};
        resetDevicesSpinner();
        cozifyAPI.getDevices(capabilities, new CozifyApiReal.JsonCallback() {
            @Override
            public void result(boolean success, String status, JSONObject resultJson) {
                if (success) {
                    devicesJson = resultJson;
                    Iterator<?> keys = resultJson.keys();
                    while (keys.hasNext()) {
                        String deviceName = (String) keys.next();
                        devicesList.add(deviceName);
                    }
                    Collections.sort(devicesList);
                    Spinner items = findViewById(R.id.spinner_devices);
                    setSpinnerItems(items, devicesList);
                    getGroups();
                } else {
                    if (resultJson != null) {
                        String message = parseMessageFromJsonResponse(resultJson);
                        setStatus("ERROR in requesting devices: " + message);
                    } else {
                        setStatus("ERROR in requesting devices (no respons): " + status);
                    }
//                    if (status.contains("408")) {
//                        revertToLocalHubConnection();
//                    } else {
                        cozifyAPI.connectRemotely();
                        enableSpinners(true);
//                    }
                }
            }
        });
    }

    String parseMessageFromJsonResponse(JSONObject resultJson) {
        if (resultJson == null) return "N/A";
        String message = resultJson.toString();
        if (resultJson.has("message")) {
            try {
                String m = resultJson.getString("message");
                if (m != null && m.length() > 0) {
                    message = m;
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return message;
    }

    void setSpinnerItems(Spinner spinner, ArrayList<String> items) {
        final Context context = CozifyAppWidgetConfigure.this;
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                context, android.R.layout.simple_spinner_dropdown_item, items);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }

    private void getHubKeys() {
        enableSpinners(false);
        disableCapabilities();
        cozifyAPI.getHubKeys(new CozifyApiReal.JsonCallback() {
            @Override
            public void result(boolean success, String status, JSONObject resultJson) {
                if (success) {
                    hubkeysJson = resultJson;
                    hubNamesJson = new JSONObject();
                    hubIdsJson = new JSONObject();
                    ArrayList<String> hubs = new ArrayList<>();
                    Iterator<?> keys = hubkeysJson.keys();
                    try {
                        while (keys.hasNext()) {
                            String hubId = (String) keys.next();
                            String hubKey = hubkeysJson.get(hubId).toString();
                            String hubName = CozifyCloudToken.parseHubNameFromToken(hubKey);
                            hubNamesJson.put(hubName, hubKey);
                            hubIdsJson.put(hubName, hubId);
                            hubs.add(hubName);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    // Populate hubs to selection list
                    Spinner items = findViewById(R.id.spinner_hubs);
                    setSpinnerItems(items, hubs);
                    resetDevicesSpinner();
                    setStatus(getString(R.string.tip_select_device_first));
                    enableSpinners(true);

                } else {
                    setStatus("Connecting to Cozify cloud failed: " + status);
                    Intent intent = new Intent(getBaseContext(),
                            CozifyWidgetSetupActivity.class);
                    startActivity(intent);
                }
            }
        });
    }

    void resetDevicesSpinner() {
        // Reset devices spinner
        devicesJson = new JSONObject();
        devicesList = new ArrayList<>();
        Spinner ditems = findViewById(R.id.spinner_devices);
        setSpinnerItems(ditems, devicesList);
    }

    private void getScenes() {
        setStatus("Wait while fetching list of Scenes..");
        cozifyAPI.getScenes(new CozifyApiReal.JsonCallback() {
            @Override
            public void result(boolean success, String status, JSONObject resultJson) {
                if (success) {

                    Iterator<?> keys = resultJson.keys();
                    try {
                        while (keys.hasNext()) {
                            String sceneName = (String) keys.next();
                            String sceneId = resultJson.get(sceneName).toString();
                            JSONObject scene = new JSONObject();
                            scene.put("id",sceneId);
                            devicesJson.put("Scene: "+sceneName, scene);
                            devicesList.add("Scene: "+sceneName);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    Spinner items = findViewById(R.id.spinner_devices);
                    setSpinnerItems(items, devicesList);
                    setStatus(getString(R.string.tip_select_device_first));
                    enableTestButtons(true);
                    enableSpinners(true);

                } else {
                    setStatus("ERROR when fetching Scenes; " + status);
//                    if (status.contains("408")) {
//                        revertToLocalHubConnection();
//                    } else {
                        cozifyAPI.connectRemotely();
                        enableSpinners(true);
//                    }
                }
            }
        });
    }

    private void getGroups() {
        setStatus("Wait while fetching list of Groups..");
        String[] capabilities = {"ON_OFF"};
        cozifyAPI.getGroups(capabilities, new CozifyApiReal.JsonCallback() {
            @Override
            public void result(boolean success, String status, JSONObject resultJson) {
                if (success) {
                    Iterator<?> keys = resultJson.keys();
                    try {
                        while (keys.hasNext()) {
                            String groupName = (String) keys.next();
                            String groupId = resultJson.get(groupName).toString();
                            JSONObject gr = new JSONObject();
                            gr.put("id",groupId);
                            devicesJson.put("Group: "+groupName, gr);
                            devicesList.add("Group: "+groupName);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    Spinner items = findViewById(R.id.spinner_devices);
                    setSpinnerItems(items, devicesList);
                } else {
                    setStatus("ERROR when fetching Groups; " + status);
                }
                getScenes();
            }
        });
    }
}
