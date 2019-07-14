package com.example.cozifywidget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.RemoteViews;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Implementation of App Widget functionality.
 */
public class CozifyAppWidget extends AppWidgetProvider {

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                int appWidgetId) {

        // Create an Intent to launch CozifyWidgetSetupActivity
        Intent intent = new Intent(context, ControlActivity.class);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.demo_app_widget);
        String device_name = PersistentStorage.getInstance().loadDeviceName(context, appWidgetId);
        if (device_name != null) {
            views.setCharSequence(R.id.control_button, "setText", device_name);
        }
        JSONObject settings = PersistentStorage.getInstance().loadSettingsJson(context, appWidgetId);
        if (settings != null) {
            try {
                int resourceForState = ControlActivity.getDeviceResourceForState(true, settings.getBoolean("isOn"), false, false);
                views.setInt(R.id.control_button, "setBackgroundResource", resourceForState);
            } catch (JSONException e) {
                Log.e("Widget", "Malformed persistent storage settings:"+e.getMessage());
            }
        }
        views.setOnClickPendingIntent(R.id.control_button, pendingIntent);
        // Tell the AppWidgetManager to perform an update on the current app widget
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // There may be multiple widgets active, so update all of them
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
    }

    @Override
    public void onEnabled(Context context) {
        return;
        // Enter relevant functionality for when the first widget is created
    }

    @Override
    public void onDisabled(Context context) {
        return;
        // Enter relevant functionality for when the last widget is disabled
    }
}

