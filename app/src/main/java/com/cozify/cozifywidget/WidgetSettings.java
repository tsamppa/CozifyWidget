package com.cozify.cozifywidget;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class WidgetSettings {
    public boolean init = false;
    private String deviceId = null;
    private String deviceName = null;
    private float textSize = 14;
    private boolean doubleSize = false;
    private Context context;
    private int widgetId;
    private JSONArray selectedCapabilities;

    public WidgetSettings(Context context, int widgetId) {
        this.selectedCapabilities = new JSONArray();
        this.context = context.getApplicationContext();
        this.widgetId = widgetId;
        String json = PersistentStorage.getInstance(context).loadWidgetSettings(widgetId);
        this.init = fromJsonString(json);
    }

    private boolean save() {
        this.init = PersistentStorage.getInstance(context).saveWidgetSettings(widgetId, toJsonString());
        return this.init;
    }


    public String toJsonString() {
        JSONObject json = new JSONObject();
        try {
            if (this.deviceId != null)
                json.put("deviceId", this.deviceId);
            if (this.deviceName != null)
                json.put("deviceName", this.deviceName);
            if (this.textSize != 0)
                json.put("selectedTextSize", this.textSize);
            json.put("doubleSize", this.doubleSize);
            json.put("selectedCapabilities", this.selectedCapabilities);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json.toString();
    }

    public boolean fromJsonString(String jsonString) {
        if (jsonString == null) return false;
        try {
            JSONObject json = new JSONObject(jsonString);
            if (json.has("deviceId"))
                this.deviceId = json.getString("deviceId");
            if (json.has("deviceName"))
                this.deviceName = json.getString("deviceName");
            if (json.has("selectedTextSize"))
                this.textSize = (float)json.getDouble("selectedTextSize");
            if (json.has("doubleSize"))
                this.doubleSize = json.getBoolean("doubleSize");
            if (json.has("selectedCapabilities"))
                this.selectedCapabilities = json.getJSONArray("selectedCapabilities");

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return true;
    }

    public String getDeviceId() {
        if (!init) return null;
        return this.deviceId;
    }

    public boolean setDeviceId(String deviceId) {
        this.deviceId = deviceId;
        return save();
    }

    public String getDeviceName() {
        if (!init) return null;
        return this.deviceName;
    }

    public boolean setDeviceName(String deviceName) {
        this.deviceName = deviceName;
        return save();
    }

    public float getTextSize() {
        return textSize;
    }

    public boolean setTextSize(float textSize) {
        this.textSize = textSize;
        return save();
    }

    public boolean setDoubleSize(boolean doubleSize) {
        this.doubleSize = doubleSize;
        return save();
    }

    public boolean getDoubleSize() {
        return this.doubleSize;
    }

    public boolean setSelectedCapabilities(JSONArray caps) {
        this.selectedCapabilities = caps;
        return save();
    }

    public JSONArray getSelectedCapabilities() {
        return this.selectedCapabilities;
    }

}