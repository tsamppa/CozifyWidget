package com.cozify.cozifywidget;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;

public class TroubleshooterRootActivity extends AppCompatActivity {

    private ArrayList<JSONObject> rulesList;
    private TextView textViewStatus;
    private CozifyApiReal cozifyAPI = new CozifyApiReal(this);
    private boolean connected = false;
    private JSONObject allRulesJson;
    private JSONObject allDevicesJson;
    private JSONObject rulesOfButtons;
    private JSONObject buttonDevices;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_troubleshooter_root);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Bundle extras = getIntent().getExtras();
        String hubKey = extras.getString("hubKey");
        cozifyAPI.selectToUseHubWithKey(hubKey, 0,  new CozifyApiReal.CozifyCallback() {
            @Override
            public void result(boolean success, String status, JSONObject jsonResponse, JSONObject jsonRequest) {
                connected = success;
                getRules();
            }
        });
    }
    private void getRules() {
        setStatus("Wait while fetching all Rules..");
        cozifyAPI.getRulesRaw(new CozifyApiReal.JsonCallback() {
            @Override
            public void result(boolean success, String status, JSONObject resultJson) {
                if (success) {
                    allRulesJson = resultJson;
                    getDevices();
                } else {
                    setStatus("ERROR fetching Rules: " + status);
                }
            }
        });
    }

    private void getDevices() {
        setStatus("Wait while fetching all Devices..");
        cozifyAPI.getDevicesRaw(new CozifyApiReal.JsonCallback() {
            @Override
            public void result(boolean success, String status, JSONObject resultJson) {
                if (success) {
                    allDevicesJson = resultJson;
                    searchRulesforButtons();
                    setStatus(getRulesOfButtonsAsString(rulesOfButtons));
                } else {
                    setStatus("ERROR fetching Devices: " + status);
                }
            }
        });
    }

    JSONObject searchButtonDevices() {
        buttonDevices = new JSONObject();
        Iterator<?> keys = allDevicesJson.keys();
        try {
            while (keys.hasNext()) {
                String deviceKey = (String) keys.next();
                JSONObject dJson = (JSONObject) allDevicesJson.get(deviceKey);
                String type = dJson.get("type").toString();
                if (type.equals("REMOTE_CONTROL") || type.equals("WALLSWITCH")) {
                    buttonDevices.put(deviceKey, dJson);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
            setStatus("Exception:" + e.getMessage());
        }
        return buttonDevices;
    }

    JSONObject searchRulesforButtons() {
        searchButtonDevices();
        rulesOfButtons = new JSONObject();
        Iterator<?> buttonDevuceIds = buttonDevices.keys();
        try {
            while (buttonDevuceIds.hasNext()) {
                String deviceId = (String) buttonDevuceIds.next();
                JSONObject dJson = (JSONObject) buttonDevices.get(deviceId);
                Iterator<?> r_keys = allRulesJson.keys();
                while (r_keys.hasNext()) {
                    String ruleId = (String) r_keys.next();
                    JSONObject rJson = (JSONObject) allRulesJson.get(ruleId);
                    JSONObject config = rJson.getJSONObject("config");
                    JSONObject inputs = config.getJSONObject("inputs");
                    Iterator<?> i_keys = inputs.keys();
                    boolean found = false;
                    while (i_keys.hasNext()) {
                        String iKey = (String) i_keys.next();
                        JSONArray iJson = (JSONArray) inputs.get(iKey);
                        for (int i = 0 ; i < iJson.length(); i++) {
                            String input = iJson.get(i).toString();
                            if (deviceId.equals(input)) {
                                rulesOfButtons.put(ruleId, deviceId);
                                found = true;
                            }
                        }
                    }
                    if (!found) {

                    }
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return rulesOfButtons;
    }

    JSONArray getRulesForDeviceButtons(String deviceId) {
        JSONArray rules = new JSONArray();
        try {
            Iterator<?> keys = rulesOfButtons.keys();
            while (keys.hasNext()) {
                String ruleId = (String) keys.next();
                String dId = rulesOfButtons.getString(ruleId);
                JSONObject ruleJson = allRulesJson.getJSONObject(ruleId);
                if (deviceId.equals(dId)) {
                    rules.put(ruleJson);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return rules;
    }

    String getRulesOfButtonsAsString(JSONObject rulesOfButtons) {
        StringBuilder rulesOfButtonsString = new StringBuilder();
        try {
            Iterator<?> keys = rulesOfButtons.keys();
            while (keys.hasNext()) {
                String ruleId = (String) keys.next();
                String deviceId = rulesOfButtons.getString(ruleId);
                JSONObject deviceJson = allDevicesJson.getJSONObject(deviceId);
                JSONObject ruleJson = allRulesJson.getJSONObject(ruleId);
                JSONObject ruleConfigJson = (JSONObject) ruleJson.get("config");
                JSONObject extras = ruleConfigJson.getJSONObject("extras");
                rulesOfButtonsString.append("\n").append(deviceJson.getString("name"));
                rulesOfButtonsString.append("-").append(getButtonLabelsOfRuleExtras(extras, deviceJson));
                rulesOfButtonsString.append(" : ").append(ruleConfigJson.getString("name"));
                rulesOfButtonsString.append("\n");
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return rulesOfButtonsString.toString();
    }

    String getButtonsWithRulesAsString(JSONObject rulesOfButtons) {
        StringBuilder buttonsWithRulesString = new StringBuilder();
        try {
            Iterator<?> keys = buttonDevices.keys();
            while (keys.hasNext()) {
                String ruleId = (String) keys.next();
                String deviceId = rulesOfButtons.getString(ruleId);
                JSONObject deviceJson = allDevicesJson.getJSONObject(deviceId);
                JSONObject ruleJson = allRulesJson.getJSONObject(ruleId);
                JSONObject ruleConfigJson = (JSONObject) ruleJson.get("config");
                JSONObject extras = ruleConfigJson.getJSONObject("extras");
                buttonsWithRulesString.append("\n").append(deviceJson.getString("name"));
                buttonsWithRulesString.append("-").append(getButtonLabelsOfRuleExtras(extras, deviceJson));
                buttonsWithRulesString.append(" : ").append(ruleConfigJson.getString("name"));
                buttonsWithRulesString.append("\n");
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return buttonsWithRulesString.toString();
    }


    String getButtonLabelsOfRuleExtras(JSONObject extras, JSONObject device) {
        StringBuilder buttonLabels = new StringBuilder();
        try {
            JSONObject onButton = (JSONObject) extras.getJSONArray("on_button").get(0);
            JSONObject offButton = (JSONObject) extras.getJSONArray("off_button").get(0);
            int onButtonId = onButton.getInt("value");
            int offButtonId = offButton.getInt("value");
            JSONArray deviceButtons = device.getJSONArray("buttons");
            for (int i = 0 ; i < deviceButtons.length(); i++) {
                JSONObject b = (JSONObject) deviceButtons.get(i);
                String label = b.getString("label");
                int id = b.getInt("id");
                if (id == onButtonId) {
                    buttonLabels.append("ON:[").append(label).append("] ");
                }
                if (id == offButtonId) {
                    buttonLabels.append("OFF:[").append(label).append("] ");
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return buttonLabels.toString();
    }

    void setStatus(String statusMessage) {
        if (textViewStatus == null) {
            textViewStatus = findViewById(R.id.textViewTroubleshootStatus);
        }
        if (textViewStatus != null) {
            textViewStatus.setText(statusMessage);
        }
    }

}