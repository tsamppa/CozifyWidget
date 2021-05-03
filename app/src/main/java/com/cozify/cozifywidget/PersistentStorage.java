package com.cozify.cozifywidget;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONException;

public class PersistentStorage {

    static final String PREFS_NAME
            = "com.cozify.android.apis.appwidget.CozifyWidgetProvider";
    static final String PREF_PREFIX_KEY = "prefix_";

    private final Context context;
    private static PersistentStorage ourInstance = null;
    public static PersistentStorage getInstance(Context context) {
        if (ourInstance == null)
            ourInstance = new PersistentStorage(context);
        return ourInstance;
    }

    private PersistentStorage(Context context) {
        this.context = context.getApplicationContext();
    }

    public void saveCloudSettings(String settings) {
        SharedPreferences.Editor prefs = context.getSharedPreferences(PREFS_NAME, 0).edit();
        prefs.putString(PREF_PREFIX_KEY + "cloudSettings", settings);
        prefs.apply();
    }

    public String loadCloudSettings() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);
        return prefs.getString(PREF_PREFIX_KEY + "cloudSettings", null);
    }

    public void saveApiSettings(int appWidgetId, String settings) {
        SharedPreferences.Editor prefs = context.getSharedPreferences(PREFS_NAME, 0).edit();
        prefs.putString(PREF_PREFIX_KEY + "apiSettings_" + appWidgetId, settings);
        prefs.apply();
    }

    public String loadApiSettings(int appWidgetId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);
        return prefs.getString(PREF_PREFIX_KEY + "apiSettings_" + appWidgetId, null);
    }

    public void saveWidgetSettings(int appWidgetId, String state) {
        SharedPreferences.Editor prefs = context.getSharedPreferences(PREFS_NAME, 0).edit();
        prefs.putString(PREF_PREFIX_KEY + "widgetSettings_" + appWidgetId, state);
        prefs.apply();
    }

    public String loadWidgetSettings(int appWidgetId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);
        return prefs.getString(PREF_PREFIX_KEY + "widgetSettings_" + appWidgetId, null);
    }

    public void saveControlState(int appWidgetId, String state) {
        SharedPreferences.Editor prefs = context.getSharedPreferences(PREFS_NAME, 0).edit();
        prefs.putString(PREF_PREFIX_KEY + "controlState_" + appWidgetId, state);
        prefs.apply();
    }

    public String loadControlState(int appWidgetId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);
        return prefs.getString(PREF_PREFIX_KEY + "controlState_" + appWidgetId, null);
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

    public void saveDeviceState(int appWidgetId, CozifySceneOrDeviceState state) {
        if (state == null) return;
        SharedPreferences.Editor prefs = context.getSharedPreferences(PREFS_NAME, 0).edit();
        try {
            String strState = state.toJson().toString();
            prefs.putString(PREF_PREFIX_KEY + "state_" + appWidgetId, strState);
            prefs.apply();
        } catch (JSONException e) {
            e.printStackTrace();
        }
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

    public void saveDesiredState(int appWidgetId, CozifySceneOrDeviceState state) {
        if (state == null) return;
        SharedPreferences.Editor prefs = context.getSharedPreferences(PREFS_NAME, 0).edit();
        try {
            String strState = state.toJson().toString();
            prefs.putString(PREF_PREFIX_KEY + "desiredState_" + appWidgetId, strState);
            prefs.apply();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public String loadCachePollData(String hubId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);
        String cachedPollData = prefs.getString(PREF_PREFIX_KEY + "cachedPollData_" + hubId, null);
        return cachedPollData;
    }

    public void saveCachePollData(String hubId, String cachedPollData) {
        SharedPreferences.Editor prefs = context.getSharedPreferences(PREFS_NAME, 0).edit();
        prefs.putString(PREF_PREFIX_KEY + "cachedPollData_" + hubId, cachedPollData);
        prefs.commit();
    }

}
