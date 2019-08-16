package com.cozify.cozifywidget;

import android.app.Activity;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RemoteViews;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.textfield.TextInputLayout;

import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Locale;

public class CozifyAppWidgetConfigure extends Activity {
    public static final String SHARED_PRES = "com.cozify.android.apis.appwidget.CozifyWidgetProvider";
    public static final String KEY_BUTTON_TEXT = "Control device";
    private int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;

    private static CozifyApiReal cozifyAPI = new CozifyApiReal();

    String cloudtoken;
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

    JSONObject hubkeysJson;
    JSONObject hubNamesJson;
    JSONObject devicesJson;
    ArrayList<String> devicesList;


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

        Intent resultValue = new Intent();
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
        setResult(RESULT_CANCELED, resultValue);

        if (mAppWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            ShowErrorMessage("Invalid Widget ID", "mAppWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID");
            finish();
        }

        devicesList = new ArrayList<>();

        PackageInfo pInfo = null;
        try {
            pInfo = context.getPackageManager().getPackageInfo(getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        if (pInfo != null) {
            TextView textViewVersion = findViewById(R.id.version);
            textViewVersion.setText(String.format(Locale.ENGLISH,"Cozify Widgets Version %s (%d)", pInfo.versionName, pInfo.versionCode));
        }
        buttonCreate = findViewById(R.id.create_button);
        buttonCreate.setEnabled(false);
        cloudtoken =  PersistentStorage.getInstance().loadCloudToken(context);
        if (cloudtoken != null) {
            setAuthHeader();
            String tokeninfo = getDecodedJwt(cloudtoken);
        }
        // Find the Device Name EditText
        editTextDeviceName = findViewById(R.id.device_name_edit);
        textInputLayoutDeviceName = findViewById(R.id.device_name);
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
                        setAuthHeader();
                        getDevices();
                    } catch (JSONException e) {
                        return;
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                enableTestButtons(false);
            }

        });

        Spinner devicessSpinner = findViewById(R.id.spinner_devices);
        devicessSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                Spinner spinner = findViewById(R.id.spinner_devices);
                Object selectedItem = spinner.getSelectedItem();
                if (selectedItem != null) {
                    selectedDeviceName = selectedItem.toString();
                    selectedDeviceId = getDeviceIdForName(selectedDeviceName);
                    enableTestButtons(true);
                    buttonCreate.setEnabled(true);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                enableTestButtons(false);
            }
        });
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
            id = (String) devicesJson.get(deviceName);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return id;
    }


    private void setAuthHeader() {
        cozifyAPI.setCloudToken(cloudtoken);
        if (selectedHubKey != null) {
            cozifyAPI.setHubKey(selectedHubKey);
        }
    }

    private void revertToLocalHubConnection() {
        cozifyAPI.listHubs(new CozifyApiReal.StringCallback() {
            @Override
            public void result(boolean success, String message, String result) {
                if (success) {
                    hubLanIp = result.substring(2, result.length()-2);
                    cozifyAPI.setHubLanIp(hubLanIp);
                    cozifyAPI.connectLocally();
                    getDevices();
                } else {
                    setStatus(message);
                }
            }
        });
    }

    public void createWidget(View w) {
        Context context = this;

        String device_name = editTextDeviceName.getText().toString();

        if (selectedDeviceId.length() > 0) {
            selectedDeviceShortName =  selectedDeviceName;
            if (device_name.length() > 0) {
                selectedDeviceShortName = device_name; // override name
            }

            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);

            // Save device ID for control
            PersistentStorage.getInstance().saveHubKey(context, mAppWidgetId, selectedHubKey);
            PersistentStorage.getInstance().saveDeviceId(context, mAppWidgetId, selectedDeviceId);
            PersistentStorage.getInstance().saveDeviceName(context, mAppWidgetId, selectedDeviceShortName);
            PersistentStorage.getInstance().saveSettings(context, mAppWidgetId, false, false, false, false, true);

            // Make sure we pass back the original appWidgetId
            Intent intent = new Intent(this, ControlActivity.class);
            intent.setAction(Long.toString(System.currentTimeMillis()));
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
            PendingIntent pendingIntent;
            pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            Log.d("PENDING DEBUG","PendingIntent at createWidget: "+mAppWidgetId);
            RemoteViews views = new RemoteViews(this.getPackageName(), R.layout.demo_app_widget);
            views.setOnClickPendingIntent(R.id.control_button, pendingIntent);
            views.setCharSequence(R.id.control_button, "setText", selectedDeviceShortName);
//            ComponentName provider = new ComponentName(context, CozifyAppWidget.class);
            appWidgetManager.updateAppWidget(mAppWidgetId, views);

            Intent resultValue = new Intent();
            resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
            setResult(RESULT_OK, resultValue);
            ShowMessage("Widget "+mAppWidgetId+" created for controlling "+selectedDeviceShortName);
            updateDeviceState(context, appWidgetManager, mAppWidgetId);
            finish();
        } else {
            String error = "Please enter short device name";
            ShowErrorMessage(error, "selectedDeviceId.length() <= 0: "+selectedDeviceId.length());
            if (textInputLayoutDeviceName != null) {
                textInputLayoutDeviceName.setErrorEnabled(true);
                textInputLayoutDeviceName.setError(error);
            }
        }
    }

    private void ShowMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        Log.i("Widget:"+mAppWidgetId, message);
    }

    private void ShowErrorMessage(String errorMessage, String details) {
        ShowMessage(errorMessage);
        Log.e("Widget: "+mAppWidgetId+" ERROR details:", details);
    }


    private void updateDeviceState(final Context context, final AppWidgetManager appWidgetManager,
                                   final int appWidgetId) {
        String deviceId = PersistentStorage.getInstance().loadDeviceId(context, appWidgetId);
        if (deviceId == null) {
            return;
        }
        cozifyAPI.getSceneOrDeviceState(deviceId, new CozifyApiReal.CozifyCallback() {
            @Override
            public void result(boolean success, String status, JSONObject resultJson, JSONObject requestJson) {
                if (success) {
                    CozifySceneOrDeviceState state = new CozifySceneOrDeviceState();
                    state.fromJson(resultJson);
                    RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.demo_app_widget);
                    int resourceForState = ControlActivity.getDeviceResourceForState(true, state.isOn, false, false, false, false, false);
                    views.setInt(R.id.control_button, "setBackgroundResource", resourceForState);
                    PersistentStorage.getInstance().saveSettings(context, appWidgetId,  state.isOn, false, false, false, true);
                    appWidgetManager.updateAppWidget(appWidgetId, views);
                } else {
                    RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.demo_app_widget);
                    int resourceForState = ControlActivity.getDeviceResourceForState(false, false, false, false, false, false, false);
                    views.setInt(R.id.control_button, "setBackgroundResource", resourceForState);
                }
            }
        });
    }

    View.OnClickListener mOnClickListenerTestOn = new View.OnClickListener() {
        public void onClick(View v) {
            if (selectedDeviceId.length() > 0) {
                boolean isScene = selectedDeviceName.contains("Scene:");
                cozifyAPI.controlOnOff(selectedDeviceId, !isScene, true, new CozifyApiReal.CozifyCallback(){
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
            if (selectedDeviceId.length() > 0) {
                boolean isScene = selectedDeviceName.contains("Scene:");
                cozifyAPI.controlOnOff(selectedDeviceId, !isScene, false, new CozifyApiReal.CozifyCallback(){
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
        Spinner ds = findViewById(R.id.spinner_devices);
        ds.setEnabled(enable);
        Spinner hs = findViewById(R.id.spinner_hubs);
        hs.setEnabled(enable);
    }

    private void getDevices() {
        setStatus("Wait while fetching list of devices..");
        enableSpinners(false);
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

                    Spinner items = findViewById(R.id.spinner_devices);
                    setSpinnerItems(items, devicesList);
                    getScenes();
                } else {
                    setStatus("ERROR in requesting devices: "+status);
                    if (status.contains("408")) {
                        revertToLocalHubConnection();
                    } else {
                        cozifyAPI.connectRemotely();
                        enableSpinners(true);
                    }
                }
            }
        });
    }

    void setSpinnerItems(Spinner spinner, ArrayList<String> items) {
        final Context context = CozifyAppWidgetConfigure.this;
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                context, android.R.layout.simple_spinner_dropdown_item, items);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }

    private void getHubKeys() {
        enableSpinners(false);
        cozifyAPI.getHubKeys(new CozifyApiReal.JsonCallback() {
            @Override
            public void result(boolean success, String status, JSONObject resultJson) {
                if (success) {
                    hubkeysJson = resultJson;
                    hubNamesJson = new JSONObject();
                    ArrayList<String> hubs = new ArrayList<String>();
                    Iterator<?> keys = hubkeysJson.keys();
                    try {
                        while (keys.hasNext()) {
                            String hubId = (String) keys.next();
                            String hubKey = hubkeysJson.get(hubId).toString();
                            String hubName = parseHubNameFromToken(hubKey);
                            hubNamesJson.put(hubName, hubKey);
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
                }
            }
        });
    }

    void resetDevicesSpinner() {
        // Reset devices spinner
        devicesJson = new JSONObject();
        devicesList = new ArrayList<String>();
        Spinner ditems = findViewById(R.id.spinner_devices);
        setSpinnerItems(ditems, devicesList);
    }

    private void getScenes() {
        cozifyAPI.getScenes(new CozifyApiReal.JsonCallback() {
            @Override
            public void result(boolean success, String status, JSONObject resultJson) {
                if (success) {

                    Iterator<?> keys = resultJson.keys();
                    try {
                        while (keys.hasNext()) {
                            String sceneName = (String) keys.next();
                            String sceneId = resultJson.get(sceneName).toString();
                            devicesJson.put("Scene: "+sceneName, sceneId);
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
                    setStatus("ERROR when fetching scenes; " + status);
                    if (status.contains("408")) {
                        revertToLocalHubConnection();
                    } else {
                        cozifyAPI.connectRemotely();
                        enableSpinners(true);
                    }
                }
            }
        });
    }


    private String parseHubNameFromToken(String token) {
        String hubName = "";
        try {
            JSONObject json = new JSONObject(getDecodedJwt(token));
            hubName = json.getString("hub_name");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return hubName;
    }

    public String getDecodedJwt(String jwt) {
        String result0;
        String result1;
        String result2;
        String[] parts = jwt.split("[.]");
        try {
            byte[] decodedBytes0 = Base64.decode(parts[0], Base64.URL_SAFE);
            result0 =  new String(decodedBytes0, StandardCharsets.UTF_8);
            byte[] decodedBytes1 = Base64.decode(parts[1], Base64.URL_SAFE);
            result1 =  new String(decodedBytes1, StandardCharsets.UTF_8);
            byte[] decodedBytes2 = Base64.decode(parts[2], Base64.URL_SAFE);
            result2 =  new String(decodedBytes2, StandardCharsets.UTF_8);
        } catch(Exception e) {
            throw new RuntimeException("Couldnt decode jwt", e);
        }
        return result1;
    }
}
