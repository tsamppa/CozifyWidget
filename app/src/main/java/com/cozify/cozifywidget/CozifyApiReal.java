package com.cozify.cozifywidget;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class CozifyApiReal {

        private JsonAPI httpAPI;
        private JsonAPI localHttpAPI;
        private String cloudBaseUrl;
        private String localBaseUrl;
        private String hubLanIp;
        private String apiver;
        private String cloudtoken = "";
        private String hubKey = "";
        private boolean remoteConnection = true;
        private Map<String, CozifySceneOrDeviceState> deviceStates = new HashMap<String, CozifySceneOrDeviceState>();
        private long deviceStateTimestamp = 0;

        public CozifyApiReal() {
            httpAPI = new JsonAPI();
            localHttpAPI = new JsonAPI();
            cloudBaseUrl = "https://api.cozify.fi/ui/0.2/hub/remote/";
            localBaseUrl = null;
            apiver = "1.11";
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
            //Log.i(method, data);
        }

        public void setHubLanIp(String hubLanIp) {
            this.hubLanIp = hubLanIp;
            if (hubLanIp != null && hubLanIp.length() > 0) {
                localBaseUrl = "http://" + hubLanIp + ":8893/";
            } else {
                localBaseUrl = null;
            }
        }

        public String getHubLanIp() {
            return hubLanIp;
        }

        public void setCloudToken(String cloudtoken) {
            this.cloudtoken = cloudtoken;
            setHeaders();
        }

        public void setApiVersion(String version) {
            apiver = version;
        }

        public void setHubKey(String hubKey, final CozifyCallback cbConnected) {
            this.hubKey = hubKey;
            setHeaders();
            if (localBaseUrl == null) {
                listHubs(new StringCallback() {
                    @Override
                    public void result(boolean success, String status, String resultString) {
                        if (success) {
                            if (resultString.length() > 2) {
                                String hubLanIp = resultString.substring(2, resultString.length() - 2);
                                setHubLanIp(hubLanIp);
                                if (cbConnected != null)
                                    cbConnected.result(true, "Connected successfully", null, null);
                                return;
                            } else {
                                setHubLanIp("");
                            }
                        }
                        if (cbConnected != null)
                            cbConnected.result(false, "Connection failed", null, null);
                    }
                });
            }
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
                                        JSONObject jsonResult = new JSONObject(stringResult);
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
                                                devicesJson.put(deviceName,deviceKey);
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


        public void getHubKeys(final JsonCallback cb) {
            String url = "https://api.cozify.fi/ui/0.2/user/hubkeys";
            httpAPI.get(url, new JsonAPI.JsonCallback() {
                        @Override
                        public void onResponse(int statusCode, JSONObject jsonResult) {
                            if (statusCode == 200) {
                                cb.result(true, "OK: "+statusCode, jsonResult);
                            } else {
                                cb.result(false, "Status code: "+statusCode, jsonResult);
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
                        public void onResponse(int statusCode, JSONObject json) {
                            if (statusCode == 200) {
                                cb.result(true, "OK: "+statusCode, json);
                            } else {
                                cb.result(false, "Status code: "+statusCode, json);
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

        public void getSceneOrDeviceState(final String device_id, final CozifyCallback cb) {
            final String url = completeUrl("/hub/poll?ts="+deviceStateTimestamp);
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
                                getHttpAPI().get(url, new JsonAPI.JsonCallback() {
                                            @Override
                                            public void onResponse(int statusCode, JSONObject result) {
                                                if (statusCode == 200) {
                                                    parsePoll(request, device_id, statusCode, result, cb);
                                                } else {
                                                    cb.result(false, "Status code " + statusCode, result, request);
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
                    //deviceStateTimestamp = timestamp;
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
    }
