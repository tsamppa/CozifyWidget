package com.cozify.cozifywidget;

import androidx.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Locale;

public class CozifyDeviceSensor extends CozifyDeviceState {
    private double value = 0;
    @CozifySensorState private int type = COZIFY_SENSOR_TYPE_UNDEFINED;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({COZIFY_SENSOR_TYPE_UNDEFINED,
            COZIFY_SENSOR_TYPE_TEMPERATURE,
            COZIFY_SENSOR_TYPE_CO2,
            COZIFY_SENSOR_TYPE_HUMIDITY,
            COZIFY_SENSOR_TYPE_LUX,
            COZIFY_SENSOR_TYPE_WATT
    })
    private @interface CozifySensorState {}

    private static final int COZIFY_SENSOR_TYPE_UNDEFINED = 0;
    private static final int COZIFY_SENSOR_TYPE_TEMPERATURE = 1;
    private static final int COZIFY_SENSOR_TYPE_CO2 = 2;
    private static final int COZIFY_SENSOR_TYPE_HUMIDITY = 3;
    private static final int COZIFY_SENSOR_TYPE_LUX = 4;
    private static final int COZIFY_SENSOR_TYPE_WATT = 5;

    public boolean setValue(double value) {
        this.value = value;
        saveDeviceState();
        return true;
    }

    public String getMeasurement() {
        switch (type) {
            case COZIFY_SENSOR_TYPE_UNDEFINED: {
                return "";
            }
            case COZIFY_SENSOR_TYPE_TEMPERATURE: {
                return String.format(Locale.ENGLISH, "%.1f C", value);
            }
            case COZIFY_SENSOR_TYPE_CO2: {
                return String.format(Locale.ENGLISH, "%.0f ppm", value);
            }
            case COZIFY_SENSOR_TYPE_HUMIDITY: {
                return String.format(Locale.ENGLISH, "%.0f %%", value);
            }
            case COZIFY_SENSOR_TYPE_LUX: {
                return String.format(Locale.ENGLISH, "%.0f lx", value);
            }
            case COZIFY_SENSOR_TYPE_WATT: {
                return String.format(Locale.ENGLISH, "%.0f W", value);
            }
        }
        return "";
    }
}