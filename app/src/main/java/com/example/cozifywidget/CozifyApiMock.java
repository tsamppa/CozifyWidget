package com.example.cozifywidget;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class CozifyApiMock extends CozifyAPI {

        private String cloudBaseUrl;
        private String localBaseUrl;
        private String apiver;
        private String cloudtoken = "";
        private String hubKey = "";
        private boolean remoteConnection = true;
        private static CozifyApiMock ourInstance = new CozifyApiMock();
        private Map<String, CozifySceneOrDeviceState> deviceStates = new HashMap<String, CozifySceneOrDeviceState>();
        private long deviceStateTimestamp = 0;

        public static CozifyApiMock getInstance() {
            return ourInstance;
        }

        private CozifyApiMock() {
        }

        public void setHubLanIp(String hubLanIp) {
        }

        public void setCloudToken(String cloudtoken) {
            this.cloudtoken = cloudtoken;
            setHeaders();
        }

        public void setHubKey(String hubKey) {
        }

        public void connectLocally() {
        }

        public void connectRemotely() {
        }

        private void setHeaders() {
        }

        public void controlOnOff(String id, boolean isDevice, boolean on, final CozifyCallback cb) {
        }

        public boolean parseCommandIsOn(JSONObject command) throws JSONException {
            String commandString = command.getString("type");
            if (commandString.equals("CMD_DEVICE_ON")) return true;
            if (commandString.equals("CMD_DEVICE_OFF")) return false;
            if (commandString.equals("CMD_SCENE_ON")) return true;
            if (commandString.equals("CMD_SCENE_OFF")) return false;
            throw new JSONException("Command type not recognized:" + commandString);
        }

        public String parseCommandTargetId(JSONObject command) throws JSONException {
            String id = command.getString("id");
            return id;
        }

        public void sendCommand(String id, CozifyCommand command, final CozifyCallback cb) {
            controlOnOffMessage(id, command.path, command.command, cb);
        }

        private void controlOnOffMessage(String id, String url_path, String command, final CozifyCallback cb) {
            final JSONObject dataJson = new JSONObject();
            final JSONObject jsonResponse = new JSONObject();
            try {
                dataJson.put("id", id);
                dataJson.put("type", command);
            } catch (JSONException e) {
                e.printStackTrace();
                cb.result(false, "Exception:" + e.getMessage(), jsonResponse, null);
            }
            final JSONArray jsonArray = new JSONArray();
            jsonArray.put(dataJson);
            cb.result(true, "Status code MOCK", null,  dataJson);
        }


        public void getDevices(final String[] capabilities, final JsonCallback cb) {
            JSONObject devicesJson = new JSONObject();
            cb.result(true, "OK MOCK", devicesJson);
        }

        public void getScenes(final JsonCallback cb) {
            JSONObject scenesJson = new JSONObject();
            cb.result(true, "OK MOCK", scenesJson);
        }


        public void getHubKeys(final JsonCallback cb) {
            JSONObject jsonResult = new JSONObject();
            cb.result(true, "OK MOCK", jsonResult);
        }

        public void listHubs(final StringCallback cb) {
            cb.result(true, "OK MOCK", "[102.168.10.2]");
        }

        public void confirmPassword(String pw, String email_address, final StringCallback cb) {
            cb.result(true, "OK MOCK: ", "");
        }

        public void requestLogin(String emailAddress, final StringCallback cb) {
            cb.result(true, "OK MOCK ", "");
        }

        public void setDeviceCacheState(String id, boolean isOn) {
        }

        public void getSceneOrDeviceState(final String device_id, final CozifyCallback cb) {
            final JSONObject request = null;
            JSONObject stateJSon = null;
            cb.result(true, "OK MOCK", stateJSon, request);
        }
    }
