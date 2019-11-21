package com.cozify.cozifywidget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.appwidget.AppWidgetProviderInfo;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.RemoteViews;

import org.json.JSONObject;

/**
 * Implementation of App Widget functionality.
 */
public class CozifyAppWidget extends AppWidgetProvider {

    static void updateAppWidget(final Context context, final AppWidgetManager appWidgetManager,
                                final int appWidgetId) {

        // Create an Intent to launch CozifyWidgetSetupActivity
        Intent intent = new Intent(context, ControlActivity.class);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, appWidgetId, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        Log.d("PENDING DEBUG","PendingIntent at updateAppWidget: "+appWidgetId);
        final WidgetSettings widgetSettings = new WidgetSettings(context, appWidgetId);
        if (widgetSettings.init && widgetSettings.getDeviceId() != null) {
            int layout = R.layout.appwidget_button;
            AppWidgetProviderInfo pi = appWidgetManager.getAppWidgetInfo(appWidgetId);
            if (pi != null) {
                layout = pi.initialLayout;
            }
            RemoteViews views = new RemoteViews(context.getPackageName(),
                    layout);
            int bid = R.id.control_button;
            if (layout == R.layout.appwidget_button_double) {
                bid = R.id.control_button_double;
            }
            views.setOnClickPendingIntent(bid, pendingIntent);
            appWidgetManager.updateAppWidget(appWidgetId, views);

            final ControlState controlState = new ControlState(context, appWidgetId);
            if (controlState.init) {
                final CozifySceneOrDeviceStateManager stateMgr = new CozifySceneOrDeviceStateManager(context, appWidgetId);
                if (controlState.shouldUpdate()) {
                    controlState.setUpdating(true);
                    stateMgr.updateCurrentState(widgetSettings.getDeviceId(), false, new CozifyApiReal.CozifyCallback() {
                        @Override
                        public void result(boolean success, String status, JSONObject jsonResponse, JSONObject jsonRequest) {
                            controlState.setUpdating(false);
                            updateIcon(context, appWidgetId, widgetSettings, controlState,
                                    stateMgr, appWidgetManager);
                        }
                    });
                } else {
                    updateIcon(context, appWidgetId, widgetSettings, controlState,
                            stateMgr, appWidgetManager);
                }
            }
        }
    }

    private static void updateIcon(Context context, int appWidgetId, WidgetSettings widgetSettings, ControlState controlState,
                            CozifySceneOrDeviceStateManager stateMgr, AppWidgetManager appWidgetManager) {
        int resourceForState = ControlActivity.updateDeviceState(widgetSettings.getDeviceName(),
                controlState.isControlling(), controlState.isArming(), controlState.isArmed(), controlState.isControlling(),
                stateMgr, widgetSettings.getTextSize(), appWidgetId, appWidgetManager, context.getPackageName(), widgetSettings.getDoubleSize());
        AppWidgetProviderInfo pi = appWidgetManager.getAppWidgetInfo(appWidgetId);
        int layout = R.layout.appwidget_button;
        if (pi != null) {
            layout = pi.initialLayout;
        }
        RemoteViews views = new RemoteViews(context.getPackageName(), layout);
        int bid = R.id.control_button;
        if (layout == R.layout.appwidget_button_double) {
            bid = R.id.control_button_double;
        }
        views.setContentDescription(bid, context.getResources().getResourceEntryName(resourceForState));
        Log.d("ResourceForState", String.format("%s : %s (%d)", "CozifyAppWidget.updateAppWidget", context.getResources().getResourceEntryName(resourceForState), resourceForState));
        // Tell the AppWidgetManager to perform an update on the current app widget
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // There may be multiple widgets active, so update all of them
        for (int appWidgetId : appWidgetIds) {
            //Log.d("Widget:"+appWidgetId, "Updating widget in AppWidgetProvider.");
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

