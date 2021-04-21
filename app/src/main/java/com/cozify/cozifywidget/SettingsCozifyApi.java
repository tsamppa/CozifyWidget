package com.cozify.cozifywidget;

import android.content.Context;

import org.json.JSONException;
import org.json.JSONObject;

public class SettingsCozifyApi {
    public boolean init = false;
    private String apiver = null;
    private String hubName = null;
    private String hubId = null;
    private String hubLanIp = null;
    private Context context;
    private int widgetId;
    private long lastHubConnectionSinceEpochMs = 0;
    private long lastDeviceStateTimestampSinceEpochMs = 0;
    private JSONObject lastPollDataJson = null;

    public SettingsCozifyApi(Context context, int widgetId) {
        this.context = context.getApplicationContext();
        this.widgetId = widgetId;
        String json = PersistentStorage.getInstance(context).loadApiSettings(widgetId);
        init = fromJsonString(json);
    }

    private boolean save() {
        PersistentStorage.getInstance(context).saveApiSettings(widgetId, toJsonString());
        init = true;
        return init;
    }

    private String toJsonString() {
        JSONObject json = new JSONObject();
        try {
            if (apiver != null)
                json.put("apiver", apiver);
            if (hubName != null)
                json.put("hubName", hubName);
            if (hubLanIp != null)
                json.put("hubLanIp", hubLanIp);
            if (hubId != null)
                json.put("hubId", hubId);
            if (lastPollDataJson != null)
                json.put("lastPollDataJson", lastPollDataJson);
            json.put("lastHubConnectionSinceEpochMs", lastHubConnectionSinceEpochMs);
            json.put("lastDeviceStateTimestampSinceEpochMs", lastDeviceStateTimestampSinceEpochMs);

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json.toString();
    }

    private boolean fromJsonString(String jsonString) {
        if (jsonString == null) return false;
        try {
            JSONObject json = new JSONObject(jsonString);
            if (json.has("apiver"))
                apiver = json.getString("apiver");
            if (json.has("hubName"))
                hubName = json.getString("hubName");
            if (json.has("hubLanIp"))
                hubLanIp = json.getString("hubLanIp");
            if (json.has("hubId"))
                hubId = json.getString("hubId");
            if (json.has("lastPollDataJson"))
                lastPollDataJson = json.getJSONObject("lastPollDataJson");
            if (json.has("lastHubConnectionSinceEpochMs"))
                lastHubConnectionSinceEpochMs = json.getLong("lastHubConnectionSinceEpochMs");
            if (json.has("lastDeviceStateTimestampSinceEpochMs"))
                lastDeviceStateTimestampSinceEpochMs = json.getLong("lastDeviceStateTimestampSinceEpochMs");

        } catch (JSONException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public String getApiVer() {
        return apiver;
    }

    public String getHubName() {
        return hubName;
    }

    public String getHubLanIp() {
        return hubLanIp;
    }

    public String getHubId() {
        return hubId;
    }

    public JSONObject getLastPollDataJson() { return lastPollDataJson; }

    public long getHubConnectedTime() { return lastHubConnectionSinceEpochMs; }

    public long getLastDeviceStateTimestamp() { return lastDeviceStateTimestampSinceEpochMs; }

    public boolean setApiVer(String apiVer) {
        this.apiver = apiVer;
        return save();
    }

    public boolean setHubName(String hubName) {
        this.hubName = hubName;
        return save();
    }

    public boolean setHubLanIp(String hubLanIp) {
        this.hubLanIp = hubLanIp;
        return save();
    }

    public boolean setHubId(String hubId) {
        this.hubId = hubId;
        return save();
    }

    public boolean setLastPollDataJson(JSONObject lastPollDataJson) {
        this.lastPollDataJson = lastPollDataJson;
        return save();
    }

    public boolean setHubConnectedTime() {
        lastHubConnectionSinceEpochMs = System.currentTimeMillis();
        return save();
    }

    public boolean setLastDeviceStateTimestamp() {
        lastDeviceStateTimestampSinceEpochMs = System.currentTimeMillis();
        return save();
    }
}