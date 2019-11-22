package com.cozify.cozifywidget;

import android.content.Context;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;

public class CozifyCloudSettings {
    public boolean init = false;
    private String email = null;
    private String cloudToken = null;
    private Context context;
    private JSONObject hubKeys = null;


    public CozifyCloudSettings(Context context) {
        this.context = context.getApplicationContext();
        String json = PersistentStorage.getInstance(context).loadCloudSettings();
        init = fromJsonString(json);
    }

    private boolean save() {
        init = PersistentStorage.getInstance(context).saveCloudSettings(toJsonString());
        return init;
    }

    public String toJsonString() {
        JSONObject json = new JSONObject();
        try {
            if (email != null)
                json.put("email", email);
            if (cloudToken != null)
                json.put("cloudToken", cloudToken);
            if (hubKeys != null) {
                json.put("hubKeys", hubKeys);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json.toString();
    }

    public boolean fromJsonString(String jsonString) {
        if (jsonString == null) return false;
        try {
            JSONObject json = new JSONObject(jsonString);
            if (json.has("email")) {
                email = json.getString("email");
            }
            if (json.has("cloudToken")) {
                cloudToken = json.getString("cloudToken");
            }
            if (json.has("hubKeys")) {
                hubKeys = json.getJSONObject("hubKeys");
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return true;
    }

    public String getEmail() {
        return email;
    }

    public boolean setEmail(String email) {
        this.email = email;
        return save();
    }

    public String getCloudToken() {
        return cloudToken;
    }
    public String getHubKey(String hubId) {
        String hk = null;
        if (hubKeys != null && hubId != null) {
            if (hubKeys.has(hubId)) {
                try {
                    hk = hubKeys.get(hubId).toString();
                } catch (JSONException e) {
                    hk = null;
                }
            }
        }
        return hk;
    }

    public String getHubKeyByHubName(String hubName) {
        String hk = null;
        if (hubKeys != null && hubName != null) {
            Iterator<?> keys = hubKeys.keys();
            try {
                while (keys.hasNext()) {
                    String hubId = (String) keys.next();
                    String hubKey = hubKeys.get(hubId).toString();
                    String hn = CozifyCloudToken.parseHubNameFromToken(hubKey);
                    if (hn.equals(hubName)) return hubKey;
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return hk;
    }

    public boolean setCloudToken(String cloudToken) {
        this.cloudToken = cloudToken;
        return save();
    }

    public boolean setHubKeys(JSONObject hubKeys) {
        this.hubKeys = hubKeys;
        return save();
    }

}