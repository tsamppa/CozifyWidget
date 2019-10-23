package com.cozify.cozifywidget;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONException;

public class PersistentStorage {

    static final String PREFS_NAME
            = "com.cozify.android.apis.appwidget.CozifyWidgetProvider";
    static final String PREF_PREFIX_KEY = "prefix_";

    private Context context;
    private static PersistentStorage ourInstance = null;
    public static PersistentStorage getInstance(Context context) {
        if (ourInstance == null)
            ourInstance = new PersistentStorage(context);
        return ourInstance;
    }

    private PersistentStorage(Context context) {
        this.context = context.getApplicationContext();
    }

    public boolean saveCloudSettings(String settings) {
        SharedPreferences.Editor prefs = context.getSharedPreferences(PREFS_NAME, 0).edit();
        prefs.putString(PREF_PREFIX_KEY + "cloudSettings", settings);
        return prefs.commit();
    }

    public String loadCloudSettings() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);
        String settingsStr = prefs.getString(PREF_PREFIX_KEY + "cloudSettings", null);
        return settingsStr;
    }

    public boolean saveApiSettings(int appWidgetId, String settings) {
        SharedPreferences.Editor prefs = context.getSharedPreferences(PREFS_NAME, 0).edit();
        prefs.putString(PREF_PREFIX_KEY + "apiSettings_" + appWidgetId, settings);
        return prefs.commit();
    }

    public String loadApiSettings(int appWidgetId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);
        String settingsStr = prefs.getString(PREF_PREFIX_KEY + "apiSettings_" + appWidgetId, null);
        return settingsStr;
    }

    public boolean saveWidgetSettings(int appWidgetId, String state) {
        SharedPreferences.Editor prefs = context.getSharedPreferences(PREFS_NAME, 0).edit();
        prefs.putString(PREF_PREFIX_KEY + "widgetSettings_" + appWidgetId, state);
        return prefs.commit();
    }

    public String loadWidgetSettings(int appWidgetId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);
        String settingsStr = prefs.getString(PREF_PREFIX_KEY + "widgetSettings_" + appWidgetId, null);
        return settingsStr;
    }

    public boolean saveControlState(int appWidgetId, String state) {
        SharedPreferences.Editor prefs = context.getSharedPreferences(PREFS_NAME, 0).edit();
        prefs.putString(PREF_PREFIX_KEY + "controlState_" + appWidgetId, state);
        return prefs.commit();
    }

    public String loadControlState(int appWidgetId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);
        String settingsStr = prefs.getString(PREF_PREFIX_KEY + "controlState_" + appWidgetId, null);
        return settingsStr;
    }


    public CozifySceneOrDeviceState loadDeviceState(int appWidgetId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);
        String stateStr = prefs.getString(PREF_PREFIX_KEY + "state_" + appWidgetId, null);
        CozifySceneOrDeviceState state = new CozifySceneOrDeviceState();
        if (stateStr != null && state.fromJsonString(stateStr)) {
            return state;
        }
        return null;
    }

    public boolean saveDeviceState(int appWidgetId, CozifySceneOrDeviceState state) {
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

    public CozifySceneOrDeviceState loadDesiredState(int appWidgetId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);
        String stateStr = prefs.getString(PREF_PREFIX_KEY + "desiredState_" + appWidgetId, null);
        CozifySceneOrDeviceState state = new CozifySceneOrDeviceState();
        if (stateStr != null && state.fromJsonString(stateStr)) {
            return state;
        }
        return null;
    }

    public boolean saveDesiredState(int appWidgetId, CozifySceneOrDeviceState state) {
        if (state == null) return false;
        SharedPreferences.Editor prefs = context.getSharedPreferences(PREFS_NAME, 0).edit();
        try {
            String strState = state.toJson().toString();
            prefs.putString(PREF_PREFIX_KEY + "desiredState_" + appWidgetId, strState);
            return prefs.commit();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return false;
    }
}
