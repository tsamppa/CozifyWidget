package com.cozify.cozifywidget;

import android.content.Context;

import org.json.JSONException;
import org.json.JSONObject;

public class ControlState {
    public boolean init = false;
    private int state = 0;
    private long stateSinceEpochMs = 0;
    private long lastUpdateSinceEpochMs = 0;
    private boolean armedForDesiredState = false;
    private static int timeout = 10000;
    private Context context;
    private int widgetId;

    public ControlState(Context context, int widgetId) {
        this.context = context.getApplicationContext();
        this.widgetId = widgetId;
        String json = PersistentStorage.getInstance(context).loadControlState(widgetId);
        init = fromJsonString(json);
    }

    private boolean save() {
        init = PersistentStorage.getInstance(context).saveControlState(widgetId, toJsonString());
        return init;
    }

    private String toJsonString() {
        JSONObject json = new JSONObject();
        try {
            json.put("state", state);
            json.put("stateSinceEpochMs", stateSinceEpochMs);
            json.put("lastUpdateSinceEpochMs", lastUpdateSinceEpochMs);
            json.put("armedForDesiredState", armedForDesiredState);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json.toString();
    }

    private boolean fromJsonString(String jsonString) {
        if (jsonString == null)
            return false;
        try {
            JSONObject json = new JSONObject(jsonString);
            if (json.has("state")) {
                state = json.getInt("state");
            } else {
                state = 0;
            }
            if (json.has("stateSinceEpochMs")) {
                stateSinceEpochMs = json.getLong("stateSinceEpochMs");
            } else {
                stateSinceEpochMs = 0;
            }
            if (json.has("lastUpdateSinceEpochMs")) {
                lastUpdateSinceEpochMs = json.getLong("lastUpdateSinceEpochMs");
            } else {
                lastUpdateSinceEpochMs = 0;
            }
            if (json.has("armedForDesiredState"))
                armedForDesiredState = json.getBoolean("armedForDesiredState");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return true;
    }


    public boolean isUpdating() {
        if (stateSinceEpochMs == 0 || System.currentTimeMillis() - stateSinceEpochMs > timeout) {
            state = 0;
            stateSinceEpochMs = 0;
            return false;
        }
        return state == 1;
    }
    public boolean isArming() {
        if (stateSinceEpochMs == 0 || System.currentTimeMillis() - stateSinceEpochMs > timeout) {
            state = 0;
            stateSinceEpochMs = 0;
            return false;
        }
        return state == 2;
    }
    public boolean isArmed() {
        if (stateSinceEpochMs == 0 || System.currentTimeMillis() - stateSinceEpochMs > timeout) {
            state = 0;
            stateSinceEpochMs = 0;
            return false;
        }
        return state == 3;
    }
    public boolean isControlling() {
        if (stateSinceEpochMs == 0 || System.currentTimeMillis() - stateSinceEpochMs > timeout) {
            state = 0;
            stateSinceEpochMs = 0;
            return false;
        }
        return state == 4;
    }

    public boolean getArmedForDesiredState() {
        return armedForDesiredState;
    }

    public void setUpdating(boolean updating) {
        if (updating) {
            state = 1;
            stateSinceEpochMs = System.currentTimeMillis();
        } else {
            state = 0;
            stateSinceEpochMs = 0;
        }
        lastUpdateSinceEpochMs = System.currentTimeMillis();
        save();
    }

    public void setArming(boolean arming) {
        if (arming) {
            state = 2;
            stateSinceEpochMs = System.currentTimeMillis();
        } else {
            state = 0;
            stateSinceEpochMs = 0;
        }
        save();
    }

    public void setArmed(boolean armed) {
        if (armed) {
            state = 3;
            stateSinceEpochMs = System.currentTimeMillis();
        } else {
            state = 0;
            stateSinceEpochMs = 0;
        }
        save();
    }

    public void setControlling(boolean controlling) {
        if (controlling) {
            state = 4;
            stateSinceEpochMs = System.currentTimeMillis();
        } else {
            state = 0;
            stateSinceEpochMs = 0;
        }
        lastUpdateSinceEpochMs = System.currentTimeMillis();
        save();
    }

    public void setArmedForDesiredState(boolean armedForDesiredState) {
        this.armedForDesiredState = armedForDesiredState;
        save();
    }

    public boolean shouldUpdate() {
        long timeSinceLastUodate = System.currentTimeMillis() - lastUpdateSinceEpochMs;
        return ((state == 0 && timeSinceLastUodate > 20000) ||
                timeSinceLastUodate > 60000);
    }
}
