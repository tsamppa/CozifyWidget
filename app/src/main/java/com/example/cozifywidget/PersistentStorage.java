package com.example.cozifywidget;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONException;
import org.json.JSONObject;

public class PersistentStorage {

    static final String PREFS_NAME
            = "com.example.android.apis.appwidget.CozifyWidgetProvider";
    static final String PREF_PREFIX_KEY = "prefix_";

    private static PersistentStorage ourInstance = new PersistentStorage();
    public static PersistentStorage getInstance() {
        return ourInstance;
    }

    private PersistentStorage() {

    }

    public void saveEmail(Context context, String email) {
        SharedPreferences.Editor prefs = context.getSharedPreferences(PREFS_NAME, 0).edit();
        prefs.putString(PREF_PREFIX_KEY + "email", email);
        prefs.apply();
    }

    public String loadEmail(Context context, int appWidgetId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);
        return prefs.getString(PREF_PREFIX_KEY + "email", null);
    }

    public void saveCloudToken(Context context, String cloudtoken) {
        SharedPreferences.Editor prefs = context.getSharedPreferences(PREFS_NAME, 0).edit();
        prefs.putString(PREF_PREFIX_KEY + "cloudtoken", cloudtoken);
        prefs.apply();
    }

    public String loadCloudToken(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);
        return prefs.getString(PREF_PREFIX_KEY + "cloudtoken", null);
    }

    public void saveHubKey(Context context, String cloudtoken) {
        SharedPreferences.Editor prefs = context.getSharedPreferences(PREFS_NAME, 0).edit();
        prefs.putString(PREF_PREFIX_KEY + "hubKey", cloudtoken);
        prefs.apply();
    }

    public String loadHubKey(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);
        return prefs.getString(PREF_PREFIX_KEY + "hubKey", null);
    }

    public void saveDeviceId(Context context, int appWidgetId, String deviceId) {
        SharedPreferences.Editor prefs = context.getSharedPreferences(PREFS_NAME, 0).edit();
        prefs.putString(PREF_PREFIX_KEY + "deviceId_" + appWidgetId, deviceId);
        prefs.apply();
    }

    public String loadDeviceId(Context context, int appWidgetId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);
        return prefs.getString(PREF_PREFIX_KEY + "deviceId_" + appWidgetId, null);
    }

    public void saveDeviceName(Context context, int appWidgetId, String deviceName) {
        SharedPreferences.Editor prefs = context.getSharedPreferences(PREFS_NAME, 0).edit();
        prefs.putString(PREF_PREFIX_KEY + "deviceName_" + appWidgetId, deviceName);
        prefs.apply();
    }

    public String loadDeviceName(Context context, int appWidgetId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);
        String deviceName = prefs.getString(PREF_PREFIX_KEY + "deviceName_" + appWidgetId, null);
        return deviceName;
    }

    private void saveSettings(Context context, int appWidgetId, String settings) {
        SharedPreferences.Editor prefs = context.getSharedPreferences(PREFS_NAME, 0).edit();
        prefs.putString(PREF_PREFIX_KEY + "settings_" + appWidgetId, settings);
        prefs.apply();
    }

    public String loadSettings(Context context, int appWidgetId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);
        String settings = prefs.getString(PREF_PREFIX_KEY + "settings_" + appWidgetId, null);
        return settings;
    }

    public void saveSettings(Context context, int appWidgetId, boolean isOn, boolean isArmed) {
        JSONObject json = new JSONObject();
        try {
            json.put("armed", isArmed);
            json.put("isOn", isOn);
        } catch (JSONException e) {
            return;
        }
        saveSettings(context, appWidgetId, json.toString());
    }

}
