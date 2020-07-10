package com.cozify.cozifywidget;

import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

public class CozifyApiReal {

        private JsonAPI httpAPI;
        private JsonAPI localHttpAPI;
        private String cloudBaseUrl;
        private String localBaseUrl;
        private Context context = null;

        private CozifyApiSettings settingsHub;
        private CozifyCloudSettings settingsCloud;

        private boolean remoteConnection = true;
        private Map<String, CozifySceneOrDeviceState> deviceStates = new HashMap<>();
        private long deviceStateTimestamp = 0;

        public CozifyApiReal(Context context) {
            this.context = context;
            httpAPI = new JsonAPI();
            localHttpAPI = new JsonAPI();
            cloudBaseUrl = "https://api.cozify.fi/ui/0.2/hub/remote/";
            localBaseUrl = null;
        }

        public void loadState(int widgetId) {
            settingsHub = new CozifyApiSettings(context, widgetId);
            setHubLanIp(settingsHub.getHubLanIp());
            loadCloudSettings();
        }

        public void loadCloudSettings() {
            settingsCloud =  new CozifyCloudSettings(context);
            setHeaders();
        }

        public void saveHubKeysToCloudSettings(JSONObject hubKeys) {
            settingsCloud.setHubKeys(hubKeys);
        }

        public interface JsonCallback {
            void result(boolean success, String status, JSONObject resultJson);
        }

        public interface StringCallback {
            void result(boolean success, String status, String resultString);
        }

        public interface CozifyCallback {
            void result(boolean success, String status, JSONObject jsonResponse, JSONObject jsonRequest);
        }
        private void trafficLog(String method, String data) {
            Log.i(method, data);
        }

        public void setHubLanIp(String hubLanIp) {
            settingsHub.setHubLanIp(hubLanIp);
            if (hubLanIp != null && hubLanIp.length() > 0) {
                localBaseUrl = "http://" + hubLanIp + ":8893/";
            } else {
                localBaseUrl = null;
            }
        }

        public void setCloudToken(String cloudToken) {
            settingsCloud.setCloudToken(cloudToken);
            setHeaders();
        }

        public void setApiVersion(String version) {
            settingsHub.setApiVer(version);
        }

        public String getApiVersion() {
            if (settingsHub == null || !settingsHub.init) return null;
            return settingsHub.getApiVer();
        }

        public void selectToUseHubWithKey(String hubId, String hubKey, int widgetId, final CozifyCallback cbConnected) {
            loadState(widgetId);
            settingsHub.setHubId(hubId);
            settingsHub.setHubName(CozifyCloudToken.parseHubNameFromToken(hubKey));
            setHeaders();
            if (localBaseUrl == null || settingsHub.getApiVer() == null) {
                listHubs(new StringCallback() {
                    @Override
                    public void result(boolean success, String status, String resultString) {
                        if (success) {
                            if (resultString.length() > 2) {
                                String hubLanIp = resultString.substring(2, resultString.length() - 2);
                                setHubLanIp(hubLanIp);
                            } else {
                                setHubLanIp("");
                            }
                            if (settingsHub.getApiVer() == null) {
                                getHubVersion(new JsonCallback() {
                                    @Override
                                    public void result(boolean success, String status, JSONObject resultJson) {
                                        if (success) {
                                            setApiVersion(parseApiVersionFromJson(resultJson));
                                            if (cbConnected != null)
                                                cbConnected.result(true, "Connected successfully", resultJson, null);
                                        } else {
                                            if (cbConnected != null)
                                                cbConnected.result(false, "Connection failed", resultJson, null);
                                        }
                                    }
                                });
                            } else {
                                cbConnected.result(true, "Connected successfully", createJsonMesageFromString(resultString), null);
                            }
                        } else {
                            if (cbConnected != null)
                                cbConnected.result(false, "Connection failed", null, null);
                        }
                    }
                });
            } else {
                cbConnected.result(true, "Connected successfully", null, null);
            }
        }

        public JSONObject createJsonMesageFromString(String message) {
            JSONObject o = new JSONObject();
            try {
                o.put("message", message);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return o;
        }

        public String parseApiVersionFromJson(JSONObject resultJson) {
            String hubApiVersion = "1.12";
            try {
                String version = resultJson.get("version").toString();
                String[] s = version.split("[.]");
                hubApiVersion = s[0] + "." + s[1];
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return hubApiVersion;
        }

        public void connectLocally() {
            if (localBaseUrl != null && localBaseUrl.length() > 0) {
                remoteConnection = false;
            }
        }

        public void connectRemotely() {
            remoteConnection = true;
        }

        public void toggleConnectType() {
            if (remoteConnection) connectLocally();
            else connectRemotely();
        }

        private void setHeaders() {
            if (getHubKey() != null) {
                httpAPI.addHeader("X-Hub-Key", getHubKey());
                localHttpAPI.addHeader("Authorization", getHubKey());
            }
            if (settingsCloud != null && settingsCloud.init && settingsCloud.getCloudToken() != null) {
                httpAPI.addHeader("Authorization", settingsCloud.getCloudToken());
            }
        }

        public void controlOnOff(String id, int type, boolean on, final CozifyCallback cb) {
            if (type == 1) {
                if (on)
                    controlOnOffMessage(id, "/devices/command", "CMD_DEVICE_ON", cb);
                else
                    controlOnOffMessage(id, "/devices/command", "CMD_DEVICE_OFF", cb);
            } else if (type == 2) {
                if (on)
                    controlOnOffMessage(id, "/scenes/command", "CMD_SCENE_ON", cb);
                else
                    controlOnOffMessage(id, "/scenes/command", "CMD_SCENE_OFF", cb);

            } else if (type == 3){
                if (on)
                    controlOnOffMessage(id, "/groups/command", "CMD_GROUP_ON", cb);
                else
                    controlOnOffMessage(id, "/groups/command", "CMD_GROUP_OFF", cb);
            }
        }

        private String completeUrl(String url_path) {
            String url = "cc/"+ settingsHub.getApiVer()+url_path;

            if (!remoteConnection && localBaseUrl != null && localBaseUrl.length() > 0) {
                url = localBaseUrl + url;
            } else {
                url = cloudBaseUrl + url;
            }
            return url;
        }

        private JsonAPI getHttpAPI() {
            if (!remoteConnection && localBaseUrl != null) {
                return localHttpAPI;
            }
            return httpAPI;
        }

        public void sendCommand(String id, CozifyCommand command, final CozifyCallback cb) {
            controlOnOffMessage(id, command.path, command.command, cb);
        }

        private void controlOnOffMessage(String id, String url_path, String command, final CozifyCallback cb) {

            String url = completeUrl(url_path);

            final JSONObject dataJson = new JSONObject();
            try {
                dataJson.put("id", id);
                dataJson.put("type", command);
            } catch (JSONException e) {
                cb.result(false, "Exception:" + e.getMessage(), null, null);
            }
            final JSONArray jsonArray = new JSONArray();
            jsonArray.put(dataJson);

            getHttpAPI().put(url, jsonArray, new JsonAPI.StringCallback() {
                        @Override
                        public void onResponse(int statusCode, String stringResult) {
                            if (statusCode == 200) {
                                try {
                                    if (stringResult != null && !stringResult.equals("null")) {
                                        stringResult = stringResult.startsWith("[") ? stringResult.substring(1) : stringResult;
                                        stringResult = stringResult.endsWith("]") ? stringResult.substring(0, stringResult.length() - 1) : stringResult;
                                        JSONObject jsonResult;
                                        if (stringResult.startsWith("{")) {
                                            jsonResult = new JSONObject(stringResult);
                                        } else {
                                            jsonResult = new JSONObject();
                                            jsonResult.put("response", stringResult);
                                        }
                                        cb.result(true, "Status code " + statusCode, jsonResult,  dataJson);
                                    } else {
                                        cb.result(true, "Status code " + statusCode, null,  dataJson);
                                    }
                                } catch (JSONException e) {
                                    JSONObject jsonResult = new JSONObject();
                                    try {
                                        jsonResult.put("valueError", stringResult);
                                        cb.result(false, "JSON reply parse error: " + e.getMessage(), jsonResult, dataJson);
                                    } catch (JSONException e1) {
                                        cb.result(false, "JSON reply parse error: " + e.getMessage(), null, dataJson);
                                    }
                                }
                            } else {
                                remoteConnection = !remoteConnection;
                                cb.result(false, "Status code " + statusCode, null, dataJson);
                            }
                        }
                    }
            );
        }

        public void getDevices(final String[] capabilities, final JsonCallback cb) {
            String url = completeUrl("/devices");
            getHttpAPI().get(url, new JsonAPI.JsonCallback() {
                        @Override
                        public void onResponse(int statusCode, JSONObject jsonResult) {
                            if (statusCode == 200) {
                                JSONObject devicesJson = new JSONObject();
                                Iterator<?> keys = jsonResult.keys();
                                try {
                                    while (keys.hasNext()) {
                                        String deviceKey = (String) keys.next();
                                        JSONObject dJson = (JSONObject) jsonResult.get(deviceKey);
                                        String deviceName = dJson.get("name").toString();
                                        JSONObject caps = (JSONObject) dJson.get("capabilities");
                                        JSONArray device_capabilities =  (JSONArray) caps.get("values");
                                        for (int i = 0 ; i < device_capabilities.length(); i++) {
                                            String dc = device_capabilities.get(i).toString();
                                            if (Arrays.asList(capabilities).contains(dc)) {
                                                JSONObject json = new JSONObject();
                                                json.put("id", deviceKey);
                                                json.put("capabilities", device_capabilities);
                                                devicesJson.put(deviceName, json);
                                                break;
                                            }
                                        }
                                    }
                                    cb.result(true, "OK "+statusCode, devicesJson);
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                    cb.result(false, "Exception:"+e.getMessage(), jsonResult);
                                }
                            } else {
                                if (jsonResult != null) {
                                    cb.result(false, "Status code " + statusCode, jsonResult);
                                } else {
                                    cb.result(false, "Status code " + statusCode, jsonResult);
                                }
                            }
                        }
                    }
            );
        }

    public void getGroups(final String[] capabilities, final JsonCallback cb) {
        String url = completeUrl("/groups");
        getHttpAPI().get(url, new JsonAPI.JsonCallback() {
                    @Override
                    public void onResponse(int statusCode, JSONObject jsonResult) {
                        if (statusCode == 200) {
                            JSONObject devicesJson = new JSONObject();
                            Iterator<?> keys = jsonResult.keys();
                            try {
                                while (keys.hasNext()) {
                                    String deviceKey = (String) keys.next();
                                    JSONObject dJson = (JSONObject) jsonResult.get(deviceKey);
                                    String deviceName = dJson.get("name").toString();
                                    devicesJson.put(deviceName,deviceKey);
                                }
                                cb.result(true, "OK "+statusCode, devicesJson);
                            } catch (JSONException e) {
                                e.printStackTrace();
                                cb.result(false, "Exception:"+e.getMessage(), jsonResult);
                            }
                        } else {
                            if (jsonResult != null) {
                                cb.result(false, "Status code " + statusCode, jsonResult);
                            } else {
                                cb.result(false, "Status code " + statusCode, jsonResult);
                            }
                        }
                    }
                }
        );
    }

    public void getScenes(final JsonCallback cb) {
        String url = completeUrl("/scenes");
        getHttpAPI().get(url, new JsonAPI.JsonCallback() {
                    @Override
                    public void onResponse(int statusCode, JSONObject jsonResult) {
                        if (statusCode == 200) {
                            JSONObject scenesJson = new JSONObject();
                            Iterator<?> keys = jsonResult.keys();
                            try {
                                while (keys.hasNext()) {
                                    String sceneKey = (String) keys.next();
                                    JSONObject dJson = (JSONObject) jsonResult.get(sceneKey);
                                    String sceneName = dJson.get("name").toString();
                                    scenesJson.put(sceneName,sceneKey);
                                }
                                cb.result(true, "OK "+statusCode, scenesJson);
                            } catch (JSONException e) {
                                e.printStackTrace();
                                cb.result(false, "Exception:" + e.getMessage(), jsonResult);
                            }
                        } else {
                            cb.result(false, "Status code " + statusCode, jsonResult);
                        }
                    }
                }
        );
    }

    private String extractErrorMessageFromJsonResult(int statusCode, JSONObject jsonResult) {
        String error = String.format(Locale.ENGLISH, "Status code: %d", statusCode);
        try {
            if (jsonResult.has("message")) {
                error = jsonResult.getString("message");
                error += String.format(Locale.ENGLISH, "(%d)", statusCode);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return error;
    }

    public void getHubKeys(final JsonCallback cb) {
        String url = "https://api.cozify.fi/ui/0.2/user/hubkeys";
        httpAPI.get(url, new JsonAPI.JsonCallback() {
                    @Override
                    public void onResponse(int statusCode, JSONObject jsonResult) {
                        if (statusCode == 200) {
                            settingsCloud.setHubKeys(jsonResult);
                            cb.result(true, "OK: "+statusCode, jsonResult);
                        } else {
                            cb.result(false, extractErrorMessageFromJsonResult(statusCode, jsonResult), jsonResult);
                        }
                    }
                }
        );
    }

    public void listHubs(final StringCallback cb) {
        String url = "https://api.cozify.fi/ui/0.2/hub/lan_ip";
        httpAPI.string_get(url, new JsonAPI.StringCallback() {
                    @Override
                    public void onResponse(int statusCode, String result) {
                        if (statusCode == 200) {
                            cb.result(true, "OK: "+statusCode, result);
                        } else {
                            cb.result(false, "Status code: "+statusCode, result);
                        }
                    }
                }
        );
    }

    public void getHubVersion(final JsonCallback cb) {
        String url = "https://api.cozify.fi/ui/0.2/hub/remote/hub";
        httpAPI.get(url, new JsonAPI.JsonCallback() {
                    @Override
                    public void onResponse(int statusCode, JSONObject jsonResult) {
                        if (statusCode == 200) {
                            cb.result(true, "OK: "+statusCode, jsonResult);
                        } else {
                            cb.result(false, extractErrorMessageFromJsonResult(statusCode, jsonResult), jsonResult);
                        }
                    }
                }
        );
    }



    public void confirmPassword(String pw, final String email_address, final StringCallback cb) {
        String url = "https://api.cozify.fi/ui/0.2/user/emaillogin";
        JSONObject json = new JSONObject();
        try {
            json.put("email", email_address);
            json.put("password", pw);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        httpAPI.post(url, json, new JsonAPI.StringCallback() {
                    @Override
                    public void onResponse(int statusCode, String result) {
                        if (statusCode == 200) {
                            settingsCloud.setEmail(email_address);
                            cb.result(true, "OK: "+statusCode, result);
                        } else {
                            cb.result(false, "Status code: "+statusCode, result);
                        }
                    }
                }
        );
    }

    public void requestLogin(String emailAddress, final StringCallback cb) {
        String url = "https://api.cozify.fi/ui/0.2/user/requestlogin?email="+emailAddress;
        httpAPI.post(url, "", new JsonAPI.StringCallback() {
                    @Override
                    public void onResponse(int statusCode, String result) {
                        if (statusCode == 200) {
                            cb.result(true, "OK: "+statusCode, result);
                        } else {
                            cb.result(false, "Status code: "+statusCode, result);
                        }
                    }
                }
        );
    }

    public void getSceneOrDeviceState(final String device_id, boolean refresh, final CozifyCallback cb) {
        if (!refresh) {
            if (deviceStates.containsKey(device_id)) {
                long delay = System.currentTimeMillis() - deviceStateTimestamp;
                if (delay < 30000) {
                    try {
                        CozifySceneOrDeviceState deviceState = deviceStates.get(device_id);
                        if (deviceState != null) {
                            cb.result(true, "OK, CACHE HIT", deviceState.toJson(), null);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        final String command = "/hub/poll?ts="+deviceStateTimestamp;
        String url = completeUrl(command);
        final JSONObject request = new JSONObject();
        try {
            request.put("url", url);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        getHttpAPI().get(url, new JsonAPI.JsonCallback() {
                    @Override
                    public void onResponse(int statusCode, JSONObject result) {
                        if (statusCode == 200) {
                            parsePoll(request, device_id, statusCode, result, cb);
                        } else {
                            // Retry once after switching connection type
                            toggleConnectType();
                            String url2 = completeUrl(command);
                            getHttpAPI().get(url2, new JsonAPI.JsonCallback() {
                                        @Override
                                        public void onResponse(int statusCode, JSONObject jsonResult) {
                                            if (statusCode == 200) {
                                                parsePoll(request, device_id, statusCode, jsonResult, cb);
                                            } else {
                                                cb.result(false, extractErrorMessageFromJsonResult(statusCode, jsonResult), jsonResult, request);
                                            }
                                        }
                                    }
                            );
                        }
                    }
                }
        );
    }

    private void parsePoll(final JSONObject request, final String device_id, int statusCode, JSONObject result, final CozifyCallback cb) {
        try {
            JSONObject stateJson = null;
            long timestamp = result.getLong("timestamp");
            boolean fullPoll = result.getBoolean("full");
            if (fullPoll) {
                deviceStateTimestamp = timestamp;
            }
            JSONArray polls = result.getJSONArray("polls");
            for (int i = 0; i < polls.length(); i++) {
                JSONObject p = polls.getJSONObject(i);
                String type = p.getString("type");
                JSONObject targets = null;
                if (type.equals("DEVICE_DELTA")) {
                    targets = p.getJSONObject("devices");
                }
                if (type.equals("SCENE_DELTA")) {
                    targets = p.getJSONObject("scenes");
                }
                if (type.equals("GROUP_DELTA")) {
                    targets = p.getJSONObject("groups");
                }
                if (targets != null) {
                    Iterator<?> ds = targets.keys();
                    while (ds.hasNext()) {
                        String k3 = (String) ds.next();
                        JSONObject target = targets.getJSONObject(k3);
                        CozifySceneOrDeviceState state = new CozifySceneOrDeviceState();
                        state.fromPollData(target, timestamp);
                        deviceStates.put(state.id, state);
                        if (device_id.equals(state.id)) {
                            stateJson = state.toJson();
                        }
                    }
                }
            }
            if (stateJson == null) {
                CozifySceneOrDeviceState deviceState = deviceStates.get(device_id);
                if (deviceState != null) {
                    stateJson = deviceState.toJson();
                } else {
                    cb.result(false, "ERROR: Could not find device state ", request, request);
                    return;
                }
            }
            if (stateJson != null)
                trafficLog("getSceneOrDeviceState", request.toString() + " : " + stateJson.toString());
            else
                trafficLog("getSceneOrDeviceState", request.toString());
            cb.result(true, "OK " + statusCode, stateJson, request);
        } catch (JSONException e) {
            e.printStackTrace();
            cb.result(false, "Exception:" + e.getMessage(), null, request);
        }
    }

    public String getHubId() {
        if (settingsHub == null || !settingsHub.init) return null;
        return settingsHub.getHubId();
    }

    public String getHubKey() {
        if (settingsCloud == null || !settingsCloud.init) return null;
        String hubId = getHubId();
        if (hubId != null) return settingsCloud.getHubKey(hubId);
        return settingsCloud.getHubKeyByHubName(getHubName());
    }

    public String getHubName() {
        if (settingsHub == null || !settingsHub.init) return null;
        if (settingsHub.getHubName() == null && getHubId() != null) {
            settingsHub.setHubName(CozifyCloudToken.parseHubNameFromToken(getHubKey()));
        }
        return settingsHub.getHubName();
    }

    public String getCloudToken() {
        if (settingsCloud == null || !settingsCloud.init) return null;
        return settingsCloud.getCloudToken();
    }

    public String getEmail() {
        if (settingsCloud == null || !settingsCloud.init) return null;
        return settingsCloud.getEmail();
    }

    public boolean setEmail(String email) {
        return settingsCloud.setEmail(email);
    }

}

