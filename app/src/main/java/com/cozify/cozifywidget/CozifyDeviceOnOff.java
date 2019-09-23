package com.cozify.cozifywidget;

import androidx.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class CozifyDeviceOnOff extends CozifyDeviceState {
    private boolean isOn = false;

    @CozifyOnOffrState private int type = COZIFY_ONOFF_TYPE_UNDEFINED;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({COZIFY_ONOFF_TYPE_UNDEFINED,
            COZIFY_ONOFF_TYPE_DEVICE,
            COZIFY_ONOFF_TYPE_GROUP,
            COZIFY_ONOFF_TYPE_SCENE
    })
    public @interface CozifyOnOffrState {}

    private static final int COZIFY_ONOFF_TYPE_UNDEFINED = 0;
    private static final int COZIFY_ONOFF_TYPE_DEVICE = 1;
    private static final int COZIFY_ONOFF_TYPE_GROUP = 2;
    private static final int COZIFY_ONOFF_TYPE_SCENE = 3;

    public boolean isOn() {
        return isOn;
    }

    public boolean setOn(boolean on) {
        isOn = on;
        return saveDeviceState();
    }

}
