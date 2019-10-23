package com.cozify.cozifywidget;

import android.content.Context;

import org.json.JSONException;
import org.json.JSONObject;

public class CozifyApiSettings {
    public boolean init = false;
    private String apiver = null;
    private String hubKey = null;
    private String hubName = null;
    private String hubLanIp = null;
    private Context context;
    private int widgetId;

    public CozifyApiSettings(Context context, int widgetId) {
        this.context = context.getApplicationContext();
        this.widgetId = widgetId;
        String json = PersistentStorage.getInstance(context).loadApiSettings(widgetId);
        init = fromJsonString(json);
    }

    private boolean save() {
        init = PersistentStorage.getInstance(context).saveApiSettings(widgetId, toJsonString());
        return init;
    }

    private String toJsonString() {
        JSONObject json = new JSONObject();
        try {
            if (apiver != null)
                json.put("apiver", apiver);
            if (hubKey != null)
                json.put("hubKey", hubKey);
            if (hubName != null)
                json.put("hubName", hubName);
            if (hubLanIp != null)
                json.put("hubLanIp", hubLanIp);
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
            if (json.has("hubKey"))
                hubKey = json.getString("hubKey");
            if (json.has("hubName"))
                hubName = json.getString("hubName");
            if (json.has("hubLanIp"))
                hubLanIp = json.getString("hubLanIp");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return true;
    }

    public String getApiVer() {
        return apiver;
    }

    public String getHubKey() {
        return hubKey;
    }

    public String getHubName() {
        return hubName;
    }

    public String getHubLanIp() {
        return hubLanIp;
    }

    public boolean setApiVer(String apiVer) {
        this.apiver = apiVer;
        return save();
    }

    public boolean setHubKey(String hubKey) {
        this.hubKey = hubKey;
        return save();
    }

    public boolean setHubName(String hubName) {
        this.hubName = hubName;
        return save();
    }

    public boolean setHubLanIp(String hubLanIp) {
        this.hubLanIp= hubLanIp;
        return save();
    }

}