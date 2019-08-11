package com.cozify.cozifywidget;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONException;
import org.json.JSONObject;

public class PersistentStorage {

    static final String PREFS_NAME
            = "com.cozify.android.apis.appwidget.CozifyWidgetProvider";
    static final String PREF_PREFIX_KEY = "prefix_";

    private static PersistentStorage ourInstance = new PersistentStorage();
    public static PersistentStorage getInstance() {
        if (ourInstance == null)
            ourInstance = new PersistentStorage();
        return ourInstance;
    }

    private PersistentStorage() {

    }

    public boolean saveEmail(Context context, String email) {
        SharedPreferences.Editor prefs = context.getSharedPreferences(PREFS_NAME, 0).edit();
        prefs.putString(PREF_PREFIX_KEY + "email", email);
        return prefs.commit();
    }

    public String loadEmail(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);
        return prefs.getString(PREF_PREFIX_KEY + "email", null);
    }

    public boolean saveCloudToken(Context context, String cloudtoken) {
        SharedPreferences.Editor prefs = context.getSharedPreferences(PREFS_NAME, 0).edit();
        prefs.putString(PREF_PREFIX_KEY + "cloudtoken", cloudtoken);
        return prefs.commit();
    }

    public String loadCloudToken(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);
        return prefs.getString(PREF_PREFIX_KEY + "cloudtoken", null);
    }

    public boolean saveHubKey(Context context, String cloudtoken) {
        SharedPreferences.Editor prefs = context.getSharedPreferences(PREFS_NAME, 0).edit();
        prefs.putString(PREF_PREFIX_KEY + "hubKey", cloudtoken);
        return prefs.commit();
    }

    public String loadHubKey(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);
        return prefs.getString(PREF_PREFIX_KEY + "hubKey", null);
    }

    public boolean saveDeviceId(Context context, int appWidgetId, String deviceId) {
        SharedPreferences.Editor prefs = context.getSharedPreferences(PREFS_NAME, 0).edit();
        prefs.putString(PREF_PREFIX_KEY + "deviceId_" + appWidgetId, deviceId);
        return prefs.commit();
    }

    public String loadDeviceId(Context context, int appWidgetId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);
        return prefs.getString(PREF_PREFIX_KEY + "deviceId_" + appWidgetId, null);
    }

    public boolean saveDeviceName(Context context, int appWidgetId, String deviceName) {
        SharedPreferences.Editor prefs = context.getSharedPreferences(PREFS_NAME, 0).edit();
        prefs.putString(PREF_PREFIX_KEY + "deviceName_" + appWidgetId, deviceName);
        return prefs.commit();
    }

    public String loadDeviceName(Context context, int appWidgetId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);
        String deviceName = prefs.getString(PREF_PREFIX_KEY + "deviceName_" + appWidgetId, null);
        return deviceName;
    }

    private boolean saveSettings(Context context, int appWidgetId, String settings) {
        SharedPreferences.Editor prefs = context.getSharedPreferences(PREFS_NAME, 0).edit();
        prefs.putString(PREF_PREFIX_KEY + "settings_" + appWidgetId, settings);
        return prefs.commit();
    }

    public String loadSettings(Context context, int appWidgetId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);
        String settings = prefs.getString(PREF_PREFIX_KEY + "settings_" + appWidgetId, null);
        return settings;
    }

    public boolean saveSettings(Context context, int appWidgetId, boolean isOn, boolean isArmed, boolean isArming, boolean isControlling, boolean isReachable) {
        JSONObject json = new JSONObject();
        try {
            json.put("armed", isArmed);
            json.put("isOn", isOn);
            json.put("reachable", isReachable);
            json.put("controlling", isControlling);
            json.put("arming", isArming);
        } catch (JSONException e) {
            e.printStackTrace();
            return false;
        }
        return saveSettings(context, appWidgetId, json.toString());
    }

    public JSONObject loadSettingsJson(Context context, int appWidgetId) {
        String settings = loadSettings(context, appWidgetId);
        if (settings != null && settings.length() > 0) {
            try {
                return new JSONObject(settings);
            } catch (JSONException e) {
                e.printStackTrace();
                return null;
            }
        }
        return null;
    }

    public CozifySceneOrDeviceState loadState(Context context, int appWidgetId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);
        String stateStr = prefs.getString(PREF_PREFIX_KEY + "state_" + appWidgetId, null);
        CozifySceneOrDeviceState state = new CozifySceneOrDeviceState();
        if (state.fromJsonStr(stateStr)) {
            return state;
        }
        return null;
    }

    public boolean saveState(Context context, int appWidgetId, CozifySceneOrDeviceState state) {
        if (state == null) return false;
        SharedPreferences.Editor prefs = context.getSharedPreferences(PREFS_NAME, 0).edit();
        try {
            String strState = state.toJson().toString();
            prefs.putString(PREF_PREFIX_KEY + "state_" + appWidgetId, strState);
            return prefs.commit();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return false;
    }
}
