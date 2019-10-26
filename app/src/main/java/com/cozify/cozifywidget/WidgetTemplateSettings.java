package com.cozify.cozifywidget;

import android.content.Context;

public class WidgetTemplateSettings {
    public boolean init = false;
    private CozifyApiSettings cozifyApiSettings;
    private WidgetSettings widgetSettings;
    private Context context;
    private String templateName;

    public WidgetTemplateSettings(Context context, int widgetId) {
        this.context = context.getApplicationContext();
        widgetSettings = new WidgetSettings(context, widgetId);
        cozifyApiSettings = new CozifyApiSettings(context, widgetId);
        init = widgetSettings.init && cozifyApiSettings.init;
        if (init) {
            templateName = getDeviceName() + " in " + getHubName();
        }
    }

    public String getTemplateName() {
        if (!init) return "N/A";
        return templateName;
    }

    public String getDeviceId() {
        if (!init) return null;
        return widgetSettings.getDeviceId();
    }

    public String getDeviceName() {
        if (!init) return null;
        return widgetSettings.getDeviceName();
    }

    public float getTextSize() {
        return widgetSettings.getTextSize();
    }

    public boolean getDoubleSize() {
        return widgetSettings.getDoubleSize();
    }

    public String getApiVer() {
        return cozifyApiSettings.getApiVer();
    }

    public String getHubKey() {
        return cozifyApiSettings.getHubKey();
    }

    public String getHubName() {
        return cozifyApiSettings.getHubName();
    }

    public String getHubLanIp() {
        return cozifyApiSettings.getHubLanIp();
    }


}
