package com.cozify.cozifywidget;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;


/**
 * Implementation of App Widget functionality.
 */
public class CozifyAppWidget extends AppWidgetProvider {

    public static String ACTION_WIDGET_CONFIG_BUTTON_PRESSED = "ActionReceiverConfigButtonPressed";
    public static String ACTION_WIDGET_REFRESH_BUTTON_PRESSED = "ActionReceiverRefreshButtonPressed";


    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // There may be multiple widgets active, so update all of them
        for (int appWidgetId : appWidgetIds) {
            //Log.d("Widget:"+appWidgetId, "Updating widget in AppWidgetProvider.");
            Bundle options = appWidgetManager.getAppWidgetOptions(appWidgetId);
            CozifyAppWidgetConfigure.updateAppWidget(context, appWidgetManager, appWidgetId, options);
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
        CozifyAppWidgetConfigure.updateAppWidget(context, appWidgetManager, appWidgetId, newOptions);
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

