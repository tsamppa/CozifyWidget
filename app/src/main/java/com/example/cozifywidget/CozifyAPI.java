package com.example.cozifywidget;

import org.json.JSONException;
import org.json.JSONObject;

public interface CozifyAPI {
    public interface JsonCallback {
        void result(boolean success, String status, JSONObject resultJson);
    }

    public interface StringCallback {
        void result(boolean success, String status, String resultString);
    }

    public interface CozifyCallback {
        void result(boolean success, String status, JSONObject jsonResponse, JSONObject jsonRequest);
    }

    public void setHubLanIp(String hubLanIp);
    public void setCloudToken(String cloudtoken);
    public void setHubKey(String hubKey);
    public void connectLocally();
    public void connectRemotely();
    public void controlOnOff(String id, boolean isDevice, boolean on, final CozifyCallback cb);
    public boolean parseCommandIsOn(JSONObject command) throws JSONException;
    public String parseCommandTargetId(JSONObject command) throws JSONException;
    public void sendCommand(String id, CozifyCommand command, final CozifyCallback cb);
    public void getDevices(final String[] capabilities, final JsonCallback cb);
    public void getScenes(final JsonCallback cb);
    public void getHubKeys(final JsonCallback cb);
    public void listHubs(final StringCallback cb);
    public void confirmPassword(String pw, String email_address, final StringCallback cb);
    public void requestLogin(String emailAddress, final StringCallback cb);
    public void setDeviceCacheState(String id, boolean isOn);
    public void getDeviceState(final String device_id, final CozifyCallback cb);
}
