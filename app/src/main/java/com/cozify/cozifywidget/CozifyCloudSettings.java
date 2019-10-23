package com.cozify.cozifywidget;

import android.content.Context;

import org.json.JSONException;
import org.json.JSONObject;

public class CozifyCloudSettings {
    public boolean init = false;
    private String email = null;
    private String cloudToken = null;
    private Context context;

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
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json.toString();
    }

    public boolean fromJsonString(String jsonString) {
        if (jsonString == null) return false;
        try {
            JSONObject json = new JSONObject(jsonString);
            if (json.has("email"))
                email = json.getString("email");
            if (json.has("cloudToken"))
                cloudToken = json.getString("cloudToken");
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

    public boolean setCloudToken(String cloudToken) {
        this.cloudToken = cloudToken;
        return save();
    }

}