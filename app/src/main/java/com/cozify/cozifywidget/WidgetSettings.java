package com.cozify.cozifywidget;

import android.content.Context;

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

    public WidgetSettings(Context context, int widgetId) {
        this.context = context.getApplicationContext();
        this.widgetId = widgetId;
        String json = PersistentStorage.getInstance(context).loadWidgetSettings(widgetId);
        init = fromJsonString(json);
    }

    private boolean save() {
        init = PersistentStorage.getInstance(context).saveWidgetSettings(widgetId, toJsonString());
        return init;
    }


    public String toJsonString() {
        JSONObject json = new JSONObject();
        try {
            if (deviceId != null)
                json.put("deviceId", deviceId);
            if (deviceName != null)
                json.put("deviceName", deviceName);
            if (textSize != 0)
                json.put("textSize", textSize);
            json.put("doubleSize", doubleSize);
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
                deviceId = json.getString("deviceId");
            if (json.has("deviceName"))
                deviceName = json.getString("deviceName");
            if (json.has("textSize"))
                textSize = (float)json.getDouble("textSize");
            if (json.has("doubleSize"))
                doubleSize = json.getBoolean("doubleSize");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return true;
    }

    public String getDeviceId() {
        if (!init) return null;
        return deviceId;
    }

    public boolean setDeviceId(String deviceId) {
        this.deviceId = deviceId;
        return save();
    }

    public String getDeviceName() {
        if (!init) return null;
        return deviceName;
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
        return doubleSize;
    }

}