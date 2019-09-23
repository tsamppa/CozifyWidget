package com.cozify.cozifywidget;

import java.util.ArrayList;
import java.util.List;

public abstract class CozifyDeviceState {
    public boolean intialized = false;
    public boolean reachable = true;
    public long timestamp = 0;
    public String id = null;
    public String type = "";
    public String manufacturer = "";
    public String model = "";
    public String name = "";
    public String room = "";
    public List<String> capabilities = new ArrayList<String>();

    public boolean saveDeviceState() {
        return true;
    }

    public boolean isInitialized() {
        return intialized;
    }

    public boolean isReachable() {
        return reachable;
    }
}
