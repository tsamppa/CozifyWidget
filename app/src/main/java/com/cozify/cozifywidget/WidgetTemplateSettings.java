package com.cozify.cozifywidget;

import android.content.Context;

public class WidgetTemplateSettings {
    public boolean init = false;
    private SettingsCozifyApi settingsCozifyApi;
    private SettingsWidget settingsWidget;
    private Context context;
    private String templateName;

    public WidgetTemplateSettings(Context context, int widgetId) {
        this.context = context.getApplicationContext();
        settingsWidget = new SettingsWidget(context, widgetId);
        settingsCozifyApi = new SettingsCozifyApi(context, widgetId);
        init = settingsWidget.init && settingsCozifyApi.init;
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
        return settingsWidget.getDeviceId();
    }

    public String getDeviceName() {
        if (!init) return null;
        return settingsWidget.getDeviceName();
    }

    public float getTextSize() {
        return settingsWidget.getTextSize();
    }

    public boolean getDoubleSize() {
        return settingsWidget.getDoubleSize();
    }

    public String getApiVer() {
        return settingsCozifyApi.getApiVer();
    }

    public String getHubName() {
        return settingsCozifyApi.getHubName();
    }

    public String getHubLanIp() {
        return settingsCozifyApi.getHubLanIp();
    }


}
