package com.cozify.cozifywidget;

import org.json.JSONException;
import org.json.JSONObject;

public abstract class CozifyAPI {
    public interface JsonCallback {
        void result(boolean success, String status, JSONObject resultJson);
    }

    public interface StringCallback {
        void result(boolean success, String status, String resultString);
    }

    public interface CozifyCallback {
        void result(boolean success, String status, JSONObject jsonResponse, JSONObject jsonRequest);
    }

    abstract public void setHubLanIp(String hubLanIp);
    abstract public void setCloudToken(String cloudtoken);
    abstract public void setHubKey(String hubKey);
    abstract public void connectLocally();
    abstract public void connectRemotely();
    abstract public void controlOnOff(String id, boolean isDevice, boolean on, final CozifyCallback cb);
    abstract public boolean parseCommandIsOn(JSONObject command) throws JSONException;
    abstract public String parseCommandTargetId(JSONObject command) throws JSONException;
    abstract public void sendCommand(String id, CozifyCommand command, final CozifyCallback cb);
    abstract public void getDevices(final String[] capabilities, final JsonCallback cb);
    abstract public void getScenes(final JsonCallback cb);
    abstract public void getHubKeys(final JsonCallback cb);
    abstract public void listHubs(final StringCallback cb);
    abstract public void confirmPassword(String pw, String email_address, final StringCallback cb);
    abstract public void requestLogin(String emailAddress, final StringCallback cb);
    abstract public void setDeviceCacheState(String id, boolean isOn);
    abstract public void getSceneOrDeviceState(final String device_id, final CozifyCallback cb);

}
