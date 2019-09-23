package com.cozify.cozifywidget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.RemoteViews;

/**
 * Implementation of App Widget functionality.
 */
public class CozifyAppWidget extends AppWidgetProvider {

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                int appWidgetId) {

        // Create an Intent to launch CozifyWidgetSetupActivity
        Intent intent = new Intent(context, ControlActivity.class);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, appWidgetId, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        Log.d("PENDING DEBUG","PendingIntent at updateAppWidget: "+appWidgetId);
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.appwidget_button);
        views.setOnClickPendingIntent(R.id.control_button, pendingIntent);

        CozifySceneOrDeviceStateManager stateMgr = new CozifySceneOrDeviceStateManager(context, appWidgetId);
        String device_name = PersistentStorage.getInstance().loadDeviceName(context, appWidgetId);
        float textSize = PersistentStorage.getInstance().loadTextSize(context, appWidgetId);
        int resourceForState = ControlActivity.updateDeviceState(device_name, false, false, false, false,
                stateMgr, textSize, appWidgetId, appWidgetManager, context.getPackageName());
        views.setContentDescription(R.id.control_button,  context.getResources().getResourceEntryName(resourceForState));
        Log.d("ResourceForState", String.format("%s : %s (%d)", "CozifyAppWidget.updateAppWidget", context.getResources().getResourceEntryName(resourceForState), resourceForState));
        // Tell the AppWidgetManager to perform an update on the current app widget
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // There may be multiple widgets active, so update all of them
        for (int appWidgetId : appWidgetIds) {
            Log.i("Widget:"+appWidgetId, "Updating widget in AppWidgetProvider.");
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {

        super.onReceive(context, intent);
    }

    @Override
    public void onEnabled(Context context) {
        // Enter relevant functionality for when the first widget is created
    }

    @Override
    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget is disabled
    }
}

