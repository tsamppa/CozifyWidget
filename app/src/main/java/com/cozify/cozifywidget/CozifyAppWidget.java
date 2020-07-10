package com.cozify.cozifywidget;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.appwidget.AppWidgetProviderInfo;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import org.json.JSONObject;


/**
 * Implementation of App Widget functionality.
 */
public class CozifyAppWidget extends AppWidgetProvider {

    public static String ACTION_WIDGET_CONFIG_BUTTON_PRESSED = "ActionReceiverConfigButtonPressed";
    public static String ACTION_WIDGET_REFRESH_BUTTON_PRESSED = "ActionReceiverRefreshButtonPressed";

    static void updateAppWidget(final Context context, final AppWidgetManager appWidgetManager,
                                final int appWidgetId, final Bundle options) {

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
                int width = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH);
                int textSize = getTextSizeForWidth(width);
                views.setFloat(R.id.control_button_measurement, "setTextSize", textSize);
                if (textSize < 11) {
                    views.setInt(R.id.widget_button_refresh, "setVisibility", View.GONE);
                } else {
                    views.setInt(R.id.widget_button_refresh, "setVisibility", View.VISIBLE);
                    views.setOnClickPendingIntent(R.id.widget_button_refresh, CozifyAppWidgetConfigure.createPendingIntentForWidgetClick(context, appWidgetId));
                }
                views.setOnClickPendingIntent(R.id.widget_button_config, CozifyAppWidgetConfigure.createPendingIntentForConfigClick(context, appWidgetId));
                views.setOnClickPendingIntent(R.id.control_button_measurement, CozifyAppWidgetConfigure.createPendingIntentForWidgetClick(context, appWidgetId));
            }
            views.setOnClickPendingIntent(bid, CozifyAppWidgetConfigure.createPendingIntentForWidgetClick(context, appWidgetId));
            appWidgetManager.updateAppWidget(appWidgetId, views);

            final ControlState controlState = new ControlState(context, appWidgetId);
            if (controlState.init) {
                final CozifySceneOrDeviceStateManager stateMgr = new CozifySceneOrDeviceStateManager(context, appWidgetId);
                if (controlState.shouldUpdate()) {
                    controlState.setUpdating(true);
                    updateIcon(context, appWidgetId, widgetSettings, controlState,
                            stateMgr, appWidgetManager);
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
    private static int getTextSizeForWidth(int width) {
        return (width / 5) + 2;
    }

    private static void updateIcon(Context context, int appWidgetId, WidgetSettings widgetSettings, ControlState controlState,
                            CozifySceneOrDeviceStateManager stateMgr, AppWidgetManager appWidgetManager) {
        int resourceForState = ControlActivity.updateDeviceState(widgetSettings, controlState,
                stateMgr, appWidgetId, appWidgetManager, context.getPackageName());
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
            Bundle options = appWidgetManager.getAppWidgetOptions(appWidgetId);
            updateAppWidget(context, appWidgetManager, appWidgetId, options);
        }

    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (null != intent) {
            String action = intent.getAction();
            if (action!= null) {
                try {
                    if (action.equals(ACTION_WIDGET_CONFIG_BUTTON_PRESSED)) {
                        Log.i("onReceive", ACTION_WIDGET_CONFIG_BUTTON_PRESSED);
                        Intent configIntent = new Intent(context,
                                CozifyAppWidgetConfigure.class);

                        int appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
                        Bundle extras = intent.getExtras();
                        if (extras != null) {
                            appWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
                        }
                        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
                            appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
                        }
                        configIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
                        configIntent.setClassName(context.getPackageName(), CozifyAppWidgetConfigure.class.getName());
                        configIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        context.startActivity(configIntent);

                    } else if (action.equals(ACTION_WIDGET_REFRESH_BUTTON_PRESSED)) {
                        Log.i("onReceive", ACTION_WIDGET_REFRESH_BUTTON_PRESSED);
                    } else {
                        super.onReceive(context, intent);
                    }
                } catch (NullPointerException e) {
                    super.onReceive(context, intent);
                }
            }
        }
    }

    @Override
    public void onAppWidgetOptionsChanged(Context context,
                                          AppWidgetManager appWidgetManager,
                                          int appWidgetId,
                                          Bundle newOptions) {
        updateAppWidget(context, appWidgetManager, appWidgetId, newOptions);
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions);
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

