package com.example.cozifywidget;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class CozifyAPI {
    private JsonAPI httpAPI;
    private JsonAPI localHttpAPI;
    private String cloudBaseUrl;
    private String localBaseUrl;
    private String apiver;
    private String cloudtoken = "";
    private String hubKey = "";
    private boolean remoteConnection = true;
    private static CozifyAPI ourInstance = new CozifyAPI();
    private Map<String, CozifyDeviceOrSceneState> deviceStates = new HashMap<String, CozifyDeviceOrSceneState>();
    private long deviceStateTimestamp = 0;

    public static CozifyAPI getInstance() {
        return ourInstance;
    }

    private CozifyAPI() {
        httpAPI = new JsonAPI();
        localHttpAPI = new JsonAPI();
        cloudBaseUrl = "https://api.cozify.fi/ui/0.2/hub/remote/";
        localBaseUrl = null;
        apiver = "1.11";
    }

    public void setHubLanIp(String hubLanIp) {
        if (hubLanIp != null && hubLanIp.length() > 0) {
            localBaseUrl = "http://" + hubLanIp + ":8893/";
        } else {
            localBaseUrl = null;
        }
    }

    public void setCloudToken(String cloudtoken) {
        this.cloudtoken = cloudtoken;
        setHeaders();
    }

    public void setHubKey(String hubKey) {
        this.hubKey = hubKey;
        setHeaders();
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
                }
            }
        });
    }

    private void setHeaders() {
        if (hubKey != null && hubKey.length() > 0) {
            httpAPI.addHeader("X-Hub-Key", hubKey);
            localHttpAPI.addHeader("Authorization", hubKey);
        }
        if (cloudtoken != null && cloudtoken.length() > 0) {
            httpAPI.addHeader("Authorization", cloudtoken);
        }
    }

    public void controlOnOff(String id, boolean isDevice, boolean on, final CozifyCallback cb) {
        if (isDevice) {
            if (on)
                controlOnOffMessage(id, "/devices/command", "CMD_DEVICE_ON", cb);
            else
                controlOnOffMessage(id, "/devices/command", "CMD_DEVICE_OFF", cb);
        } else {
            if (on)
                controlOnOffMessage(id, "/scenes/command", "CMD_SCENE_ON", cb);
            else
                controlOnOffMessage(id, "/scenes/command", "CMD_SCENE_OFF", cb);
        }
    }

    private String completeUrl(String url_path) {
        String url = "cc/"+apiver+url_path;

        if (!remoteConnection && localBaseUrl != null && localBaseUrl.length() > 0) {
            url = localBaseUrl + url;
        } else {
            url = cloudBaseUrl + url;
        }
        return url;
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
        String requestData = jsonArray.toString();

        httpAPI.put(url, requestData, new JsonAPI.StringCallback() {
                @Override
                public void onResponse(int statusCode, String stringResult) {
                    if (statusCode == 200) {
                        try {
                            if (stringResult != null && !stringResult.equals("null")) {
                                JSONObject jsonResult = new JSONObject(stringResult);
                                cb.result(true, "Status code " + statusCode, jsonResult,  dataJson);
                            } else {
                                cb.result(true, "Status code " + statusCode, null,  dataJson);
                            }
                        } catch (JSONException e) {
                            cb.result(false, "JSON reply parse error: " + e.getMessage(), null, dataJson);
                        }
                    } else {
                        remoteConnection = !remoteConnection;
                        cb.result(false, "Status code " + statusCode, null, dataJson);
                    }
                }
            }
        );
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


    public void getDevices(final String[] capabilities, final JsonCallback cb) {
        String url = completeUrl("/devices");
        httpAPI.get(url, new JsonAPI.JsonCallback() {
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
                                    devicesJson.put(deviceName,deviceKey);
                                }
                            }
                        }
                        cb.result(true, "OK "+statusCode, devicesJson);
                    } catch (JSONException e) {
                        e.printStackTrace();
                        cb.result(false, "Exception:"+e.getMessage(), null);
                    }
                } else {
                    cb.result(false, "Status code "+statusCode, null);
                }
            }
        }
        );
    }

    public void getScenes(final JsonCallback cb) {
        String url = completeUrl("/scenes");
        httpAPI.get(url, new JsonAPI.JsonCallback() {
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
                            cb.result(false, "Exception:" + e.getMessage(), null);
                        }
                    } else {
                        cb.result(false, "Status code " + statusCode, null);
                    }
                }
            }
        );
    }


    public void getHubKeys(final JsonCallback cb) {
        String url = "https://api.cozify.fi/ui/0.2/user/hubkeys";
        httpAPI.get(url, new JsonAPI.JsonCallback() {
                @Override
                public void onResponse(int statusCode, JSONObject jsonResult) {
                    if (statusCode == 200) {
                        cb.result(true, "OK: "+statusCode, jsonResult);
                    } else {
                        cb.result(false, "Status code: "+statusCode, null);
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
                        cb.result(false, "Status code: "+statusCode, null);
                    }
                }
            }
        );
    }

    public void confirmPassword(String pw, String email_address, final StringCallback cb) {
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
                        cb.result(true, "OK: "+statusCode, result);
                    } else {
                        cb.result(false, "Status code: "+statusCode, null);
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
                        cb.result(false, "Status code: "+statusCode, null);
                    }
                }
            }
        );
    }

    public void setDeviceCacheState(String id, boolean isOn) {
        CozifyDeviceOrSceneState deviceState = deviceStates.get(id);
        deviceState.isOn = isOn;
        deviceStates.put(id, deviceState);
    }

    public void getDeviceState(final String device_id, final JsonCallback cb) {
        long diff = System.currentTimeMillis() - deviceStateTimestamp;
        if (deviceStateTimestamp != 0 && diff < 10000) {
            CozifyDeviceOrSceneState deviceState = deviceStates.get(device_id);
            if (deviceState != null) {
                try {
                    JSONObject stateJSon = deviceState.toJson();
                    cb.result(true, "Cache hit", stateJSon);
                    return;
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
        String url = completeUrl("/hub/poll?ts="+deviceStateTimestamp);
        httpAPI.get(url, new JsonAPI.JsonCallback() {
            @Override
            public void onResponse(int statusCode, JSONObject result) {
                if (statusCode == 200) {
                    try {
                        JSONObject stateJSon = null;
                        long timestamp = result.getLong("timestamp");
                        deviceStateTimestamp = timestamp;
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
                            if (targets != null) {
                                Iterator<?> ds = targets.keys();
                                while (ds.hasNext()) {
                                    String k3 = (String) ds.next();
                                    JSONObject target = targets.getJSONObject(k3);
                                    CozifyDeviceOrSceneState state = new CozifyDeviceOrSceneState();
                                    state.fromPollData(target, timestamp);
                                    deviceStates.put(state.id, state);
                                    if (device_id.equals(state.id)) {
                                        stateJSon = state.toJson();
                                    }
                                }
                            }
                        }
                        if (stateJSon == null) {
                            CozifyDeviceOrSceneState deviceState = deviceStates.get(device_id);
                            if (deviceState != null) {
                                stateJSon = deviceState.toJson();
                            } else {
                                cb.result(false, "ERROR: Could not find device state ", null);
                            }
                        }
                        cb.result(true, "OK " + statusCode, stateJSon);
                    } catch (JSONException e) {
                        e.printStackTrace();
                        cb.result(false, "Exception:" + e.getMessage(), null);
                    }
                } else {
                    cb.result(false, "Status code " + statusCode, null);
                }
            }
        }
        );
    }
}
