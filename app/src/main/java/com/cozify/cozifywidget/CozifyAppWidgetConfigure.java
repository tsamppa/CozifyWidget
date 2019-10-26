package com.cozify.cozifywidget;

import android.app.Activity;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.RemoteViews;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.textfield.TextInputLayout;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
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
    JSONObject devicesJson;
    ArrayList<String> devicesList;
    ArrayList<WidgetTemplateSettings> templates;


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
            int layout = AppWidgetManager.getInstance(this).getAppWidgetInfo(mAppWidgetId).initialLayout;
            String size = " Creating Single Size Widget";
            if (layout == R.layout.appwidget_button_double) {
                size = " Creating Double Size Widget";
            }
            textViewVersion.setText(String.format(Locale.ENGLISH,"Cozify Widgets Version %s (%d) - %s",
                    pInfo.versionName, pInfo.versionCode, size));
        }
        buttonCreate = findViewById(R.id.create_button);
        buttonCreate.setEnabled(false);
        if (cozifyAPI.getCloudToken() == null) {
            setStatus(String.format(Locale.ENGLISH,"Error in setting cannot connect to cloud. Please re-login."));
            Intent intent = new Intent(this, CozifyWidgetSetupActivity.class);
            startActivity(intent);
            return;
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

        Spinner widgetTemplatesSpinner = findViewById(R.id.spinner_widgets);
        widgetTemplatesSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                if (!spinnersEnabled) return;
                enableTestButtons(false);
                buttonCreate.setEnabled(false);
                devicesList = new ArrayList<>();

                Spinner spinner = findViewById(R.id.spinner_widgets);
                Object selectedItem = spinner.getSelectedItem();
                if (selectedItem != null) {
                    WidgetTemplateSettings selectedTemplate = findTemplate(selectedItem.toString());
                    if (selectedTemplate != null) {
                        selectedHubName = selectedTemplate.getHubName();
                        Spinner spinnerHubs = findViewById(R.id.spinner_hubs);
                        spinnerHubs.setSelection(((ArrayAdapter)spinnerHubs.getAdapter()).getPosition(selectedHubName));
                        selectedHubKey = selectedTemplate.getHubKey();
                        selectedDeviceId = selectedTemplate.getDeviceId();
                        selectedDeviceName = selectedTemplate.getDeviceName();
                        Spinner spinnerDevices = findViewById(R.id.spinner_devices);
                        spinnerDevices.setSelection(((ArrayAdapter)spinnerHubs.getAdapter()).getPosition(selectedDeviceName));
                        selectedDeviceShortName = selectedTemplate.getDeviceName();
                        editTextDeviceName = findViewById(R.id.device_name_edit);
                        editTextDeviceName.setText(selectedDeviceShortName);
                        selectedTextSize = selectedTemplate.getTextSize();
                        RadioGroup rg = findViewById(R.id.text_size_radio_group);
                        if (selectedTextSize > 19) {
                            rg.check(R.id.text_size_large);
                        } else if (selectedTextSize < 15) {
                            rg.check(R.id.text_size_small);
                        } else {
                            rg.check(R.id.text_size_medium);
                        }
                        enableTestButtons(true);
                        buttonCreate.setEnabled(true);
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
            }

        });
        populateWidgetTemplateSpinner();

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
                        if (selectedHubKey != null) {
                            cozifyAPI.selectToUseHubWithKey(selectedHubKey, mAppWidgetId, new CozifyApiReal.CozifyCallback() {
                                @Override
                                public void result(boolean success, String status, JSONObject jsonResponse, JSONObject jsonRequest) {
                                    connected = success;
                                    if (success) {
                                        hubApiVersion = cozifyAPI.getApiVersion();
                                        getDevices();
                                    } else {
                                        String message = parseMessageFromJsonResponse(jsonResponse);
                                        setStatus(status + ": "+ message);
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

        RadioGroup rg = findViewById(R.id.text_size_radio_group);

        rg.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener()
        {
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                switch(checkedId){
                    case R.id.text_size_small:
                        selectedTextSize = 13;
                        break;
                    case R.id.text_size_medium:
                        selectedTextSize = 17;
                        break;
                    case R.id.text_size_large:
                        selectedTextSize = 22;
                        break;
                }
            }
        });
    }

    WidgetTemplateSettings findTemplate(String templateName) {
        if (templateName == null || templateName.length() < 1)
            return null;
        for (WidgetTemplateSettings t: templates) {
            if (templateName.equals(t.getTemplateName())) {
                return t;
            }
        }
        return null;
    }

    void populateWidgetTemplateSpinner() {
        Spinner items = findViewById(R.id.spinner_widgets);
        ArrayList<String> widgetsList = new ArrayList<>();
        templates = new ArrayList<>();
        setSpinnerItems(items, widgetsList);
        int[] ids1 = AppWidgetManager.getInstance(getApplication()).getAppWidgetIds(new ComponentName(this.getPackageName(), CozifyAppWidget.class.getName()));
        for (int id : ids1) {
            templates.add(new WidgetTemplateSettings(this, id));
        }
        int[] ids2 = AppWidgetManager.getInstance(getApplication()).getAppWidgetIds(new ComponentName(this.getPackageName(), CozifyAppWidgetDouble.class.getName()));
        for (int id : ids2) {
            templates.add(new WidgetTemplateSettings(this, id));
        }
        for (WidgetTemplateSettings template: templates) {
            if (template.init)
                widgetsList.add(template.getTemplateName());
        }
        setSpinnerItems(items, widgetsList);

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
            ControlState controlState = new ControlState(context, mAppWidgetId);
            controlState.setControlling(false);
            updateCurrentState(selectedDeviceId);

            // Make sure we pass back the original appWidgetId
            Intent intent = new Intent(this, ControlActivity.class);
            intent.setAction(Long.toString(System.currentTimeMillis()));
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
            PendingIntent pendingIntent;
            pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            //Log.d("PENDING DEBUG","PendingIntent at createWidget: "+mAppWidgetId);
            int layout = appWidgetManager.getAppWidgetInfo(mAppWidgetId).initialLayout;
            RemoteViews views = new RemoteViews(this.getPackageName(),
                    layout);
            int bid = R.id.control_button;
            if (layout == R.layout.appwidget_button_double) {
                bid = R.id.control_button_double;
            }
            views.setOnClickPendingIntent(bid, pendingIntent);
            views.setCharSequence(bid, "setText", selectedDeviceShortName);
            views.setFloat(bid, "setTextSize", selectedTextSize);

            appWidgetManager.updateAppWidget(mAppWidgetId, views);

            Intent resultValue = new Intent();
            resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
            setResult(RESULT_OK, resultValue);
            String hubName = cozifyAPI.parseHubNameFromToken(selectedHubKey);
            ShowMessage("Widget "+mAppWidgetId+" created for controlling "+selectedDeviceShortName +" of HUB "+hubName);
            finish();
        } else {
            String error = "Please select device";
            ShowErrorMessage(error, "selectedDeviceId.length() <= 0 or null: ");
            if (textInputLayoutDeviceName != null) {
                textInputLayoutDeviceName.setErrorEnabled(true);
                textInputLayoutDeviceName.setError(error);
            }
        }
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
                        setStatus("ERROR in requesting devices: " + status);
                    }
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
        cozifyAPI.getHubKeys(new CozifyApiReal.JsonCallback() {
            @Override
            public void result(boolean success, String status, JSONObject resultJson) {
                if (success) {
                    hubkeysJson = resultJson;
                    hubNamesJson = new JSONObject();
                    ArrayList<String> hubs = new ArrayList<>();
                    Iterator<?> keys = hubkeysJson.keys();
                    try {
                        while (keys.hasNext()) {
                            String hubId = (String) keys.next();
                            String hubKey = hubkeysJson.get(hubId).toString();
                            String hubName = cozifyAPI.parseHubNameFromToken(hubKey);
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
                    // TODO: Forward user to login
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
                    setStatus("ERROR when fetching Scenes; " + status);
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
                            String sceneId = resultJson.get(groupName).toString();
                            devicesJson.put("Group: "+groupName, sceneId);
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
