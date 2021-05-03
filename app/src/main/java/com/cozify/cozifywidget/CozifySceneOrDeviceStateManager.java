package com.cozify.cozifywidget;

import android.content.Context;
import android.os.Handler;
import android.text.format.DateFormat;
import android.util.Base64;
import android.util.Log;

import androidx.annotation.IntDef;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

public class CozifySceneOrDeviceStateManager implements Runnable {
    private CozifySceneOrDeviceState currentState = null;
    private CozifySceneOrDeviceState desiredState;
    private Handler handler;
    private Long startTime = null;
    private CozifyApiReal.CozifyCallback cbFinished = null;
    public boolean connected = false;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({CONTROL_PRCESS_STATE_INIT,
            CONTROL_PRCESS_STATE_CONTROL_LOCAL,
            CONTROL_PRCESS_STATE_CONTROL_REMOTE,
            CONTROL_PRCESS_STATE_RETRY_LOCAL,
            CONTROL_PRCESS_STATE_RETRY_REMOTE,
            CONTROL_PRCESS_STATE_COMPLETED,
            CONTROL_PRCESS_STATE_ABORTED,
            CONTROL_PRCESS_STATE_EXPIRED,
            CONTROL_PRCESS_STATE_UNREACHABLE,
            CONTROL_PRCESS_STATE_FAILURE
    })
    public @interface ControlProcessState {}

    private static final int CONTROL_PRCESS_STATE_INIT = 0;
    private static final int CONTROL_PRCESS_STATE_CONTROL_LOCAL = 1;
    private static final int CONTROL_PRCESS_STATE_CONTROL_REMOTE = 2;
    private static final int CONTROL_PRCESS_STATE_RETRY_LOCAL = 3;
    private static final int CONTROL_PRCESS_STATE_RETRY_REMOTE = 4;
    private static final int CONTROL_PRCESS_STATE_COMPLETED = 5;
    private static final int CONTROL_PRCESS_STATE_ABORTED = 6;
    private static final int CONTROL_PRCESS_STATE_EXPIRED = 7;
    private static final int CONTROL_PRCESS_STATE_UNREACHABLE = 8;
    private static final int CONTROL_PRCESS_STATE_FAILURE = 9;


    private static final int retryDelay = 10 * 1000; // 10 secs
    private static final int maxRuntime = 10*60*1000; // 10 minutes
    private int mAppWidgetId;
    private Context mContext;

    private CozifyApiReal cozifyAPI = null;

    @ControlProcessState private int state = CONTROL_PRCESS_STATE_INIT;

    CozifySceneOrDeviceStateManager(Context context, int appWidgetId) {
        handler = new Handler();
        mAppWidgetId = appWidgetId;
        mContext = context;
        cozifyAPI = new CozifyApiReal(context);
        loadState();
    }

    public void loadState() {
        currentState = PersistentStorage.getInstance(mContext).loadDeviceState(mAppWidgetId);
        desiredState = PersistentStorage.getInstance(mContext).loadDesiredState(mAppWidgetId);
        cozifyAPI.loadState(mAppWidgetId);
        if (cozifyAPI.getHubKey() != null) {
            cozifyAPI.selectToUseHubWithKey(cozifyAPI.getHubKey(), mAppWidgetId, new CozifyApiReal.CozifyCallback() {
                @Override
                public void result(boolean success, String status, JSONObject jsonResponse, JSONObject jsonRequest) {
                    connected = success;
                }
            });
            //Log.d("WIDGET-HUBKEY MATCH", String.format("Widget %d controls hub %s", mAppWidgetId, cozifyAPI.settingsHub.hubName));
        }
    }

    private void refreshHubKey() {
        if (cozifyAPI.getHubKey() != null) {
            final String hubName = cozifyAPI.getHubName();
            cozifyAPI.getHubKeys(new CozifyApiReal.JsonCallback() {
                @Override
                public void result(boolean success, String message, JSONObject jsonResult) {
                    if (success) {
                        Iterator<String> iter = jsonResult.keys();
                        while (iter.hasNext()) {
                            String hubId = iter.next();
                            try {
                                String hubKey = jsonResult.getString(hubId);
                                String hn = parseHubNameFromToken(hubKey);
                                if (hubName.equals(hn)) {
                                    cozifyAPI.setApiVersion(null); // Reset to refresh it
                                    cozifyAPI.selectToUseHubWithKey(hubKey, mAppWidgetId, new CozifyApiReal.CozifyCallback() {
                                        @Override
                                        public void result(boolean success, String status, JSONObject jsonResponse, JSONObject jsonRequest) {
                                            if (success) {
                                                connected = true;
                                            }
                                        }
                                    });
                                    return;
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            });
        }
    }

    private String parseHubNameFromToken(String token) {
        String hubName = "";
        try {
            JSONObject json = new JSONObject(getDecodedJwt(token));
            hubName = json.getString("hub_name");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return hubName;
    }

    private String getDecodedJwt(String jwt) {
        String result0;
        String result1;
        String result2;
        String[] parts = jwt.split("[.]");
        try {
            byte[] decodedBytes0 = Base64.decode(parts[0], Base64.URL_SAFE);
            result0 =  new String(decodedBytes0, StandardCharsets.UTF_8);
            byte[] decodedBytes1 = Base64.decode(parts[1], Base64.URL_SAFE);
            result1 =  new String(decodedBytes1, StandardCharsets.UTF_8);
            byte[] decodedBytes2 = Base64.decode(parts[2], Base64.URL_SAFE);
            result2 =  new String(decodedBytes2, StandardCharsets.UTF_8);
        } catch(Exception e) {
            throw new RuntimeException("Could not decode jwt", e);
        }
        return result1;
    }

    public void saveState() {
        PersistentStorage.getInstance(mContext).saveDeviceState(mAppWidgetId, currentState);
        PersistentStorage.getInstance(mContext).saveDesiredState(mAppWidgetId, desiredState);
    }

    public boolean isReady() {
        if (state == CONTROL_PRCESS_STATE_INIT) return true;
        if (isFinalState(state)) return true;
        return false;
    }

    private boolean isFinalState(@ControlProcessState int state) {
        Log.d("STATE", explainControlProcessState(state));
        switch (state) {
            case CONTROL_PRCESS_STATE_INIT:
            case CONTROL_PRCESS_STATE_CONTROL_LOCAL:
            case CONTROL_PRCESS_STATE_CONTROL_REMOTE:
            case CONTROL_PRCESS_STATE_RETRY_LOCAL:
            case CONTROL_PRCESS_STATE_RETRY_REMOTE: {
                return false;
            }
            case CONTROL_PRCESS_STATE_ABORTED:
            case CONTROL_PRCESS_STATE_COMPLETED:
            case CONTROL_PRCESS_STATE_EXPIRED:
            case CONTROL_PRCESS_STATE_UNREACHABLE:
            case CONTROL_PRCESS_STATE_FAILURE: {
                return true;
            }
        }
        throw new IllegalStateException("State machine broken");
    }


    public void run() {
        try {
            if (isFinalState(state)) {
                return;
            } else if (System.currentTimeMillis() - startTime > maxRuntime) {   // Has the time expired
                state = CONTROL_PRCESS_STATE_EXPIRED;
                handler.removeCallbacks(this);
                reportResult();
            } else {
                checkCurrentStateAndSendCommand();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String explainControlProcessState(@ControlProcessState int s) {
        switch (s) {
            case CONTROL_PRCESS_STATE_INIT: return("CONTROL_PRCESS_STATE_INIT");
            case CONTROL_PRCESS_STATE_CONTROL_LOCAL: return("CONTROL_PRCESS_STATE_CONTROL_LOCAL");
            case CONTROL_PRCESS_STATE_CONTROL_REMOTE: return("CONTROL_PRCESS_STATE_CONTROL_REMOTE");
            case CONTROL_PRCESS_STATE_RETRY_LOCAL: return("CONTROL_PRCESS_STATE_RETRY_LOCAL");
            case CONTROL_PRCESS_STATE_RETRY_REMOTE: return("CONTROL_PRCESS_STATE_RETRY_REMOTE");
            case CONTROL_PRCESS_STATE_ABORTED: return("CONTROL_PRCESS_STATE_ABORTED");
            case CONTROL_PRCESS_STATE_COMPLETED: return("CONTROL_PRCESS_STATE_COMPLETED");
            case CONTROL_PRCESS_STATE_EXPIRED: return("CONTROL_PRCESS_STATE_EXPIRED");
            case CONTROL_PRCESS_STATE_UNREACHABLE: return("CONTROL_PRCESS_STATE_UNREACHABLE");
            case CONTROL_PRCESS_STATE_FAILURE: return("CONTROL_PRCESS_STATE_FAILURE");
        }
        throw new NullPointerException("Logic error in StateManager");
    }

    private void reportResult() {
        if (cbFinished != null) {
            cbFinished.result(state == CONTROL_PRCESS_STATE_COMPLETED, explainControlProcessState(state), null, null);
        } else {
            throw new NullPointerException("Logic error in StateManager");
        }
        cbFinished = null;
    }

    public void abort() {
        state = CONTROL_PRCESS_STATE_ABORTED;
        handler.removeCallbacks(this);
        reportResult();
    }

    private boolean reportIfFinalState() {
        if (state == CONTROL_PRCESS_STATE_ABORTED)
            return true;
        if (state == CONTROL_PRCESS_STATE_EXPIRED)
            return true;
        if (!currentState.reachable) {
            state = CONTROL_PRCESS_STATE_UNREACHABLE;
            reportResult();
            return true;
        }
        boolean completed = desiredState.similarToState(currentState);
        if (completed) {
            state = CONTROL_PRCESS_STATE_COMPLETED;
            reportResult();
            return true;
        }
        return false;
    }

    public void updateCurrentState(String device_id, boolean refresh, final CozifyApiReal.CozifyCallback cb) {
        cozifyAPI.getSceneOrDeviceState(device_id, refresh, new CozifyApiReal.CozifyCallback() {
            public void result(boolean success, String status, JSONObject jsonResponse, JSONObject jsonRequest) {
                if (success) {
                    setCurrentStateFromJsonResponse(jsonResponse);
                } else {
                    try {
                        if (jsonResponse != null) {
                            String message = "N/A";
                            if (jsonResponse.has("message")) {
                                String msg = jsonResponse.getString("message");
                                if (msg.length() > 0) {
                                    message = msg;
                                }
                            }
                            if (jsonResponse.has("response")) {
                                message = jsonResponse.getString("response");
                            }
                            int errorCode = 0;
                            if (jsonResponse.has("errorCode")) {
                                errorCode = jsonResponse.getInt("errorCode");
                            }
                            if (message.startsWith("Authentication failed")
                                    || message.startsWith("Authorization failed")
                                    || message.startsWith("Connection refused")
                                    || errorCode == 401) {
                                refreshHubKey();
                            }
                            Log.e("StateManager", String.format("Error: %s STATUS: %s (jsonRequest: '%s' jsonResponse: '%s'",
                                    message, status, jsonRequest.toString(), jsonResponse.toString()));
                        } else {
                            Log.e("StateManager", String.format("Error STATUS: %s (jsonRequest: '%s'",
                                    status, jsonRequest.toString()));
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                cb.result(success, status, jsonResponse, jsonRequest);
            }
        });
    }

    private void updateCurrentStateAndRetryControlAfterDelay() {
        cozifyAPI.getSceneOrDeviceState(desiredState.id, true, new CozifyApiReal.CozifyCallback() {
            public void result(boolean success, String status, JSONObject jsonResponse, JSONObject jsonRequest) {
                if (success) {
                    setCurrentStateFromJsonResponse(jsonResponse);
                    if (reportIfFinalState()) return;
                }
                retryControlAfterDelay();
            }
        });
    }

    private void retryControlAfterDelay() {
        handler.postDelayed(this, retryDelay);
    }

    // Returns true if desired state reached
    private void sendCommand() {
        // resend command
        CozifyCommand command = currentState.getCommandTowardsDesiredState(desiredState);
        cozifyAPI.sendCommand(currentState.id, command,
                new CozifyApiReal.CozifyCallback() {
                    @Override
                    public void result(boolean success, String status, JSONObject jsonResponse, JSONObject jsonRequest) {
                        // Check state after a bit of delay
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                updateCurrentStateAndRetryControlAfterDelay();
                            }
                        }, 500 );
                    }
                });
    }

    private void checkCurrentStateAndSendCommand() {
        cozifyAPI.getSceneOrDeviceState(currentState.id, true, new CozifyApiReal.CozifyCallback() {
            public void result(boolean success, String status, JSONObject jsonResponse, JSONObject jsonRequest) {
                if (success) {
                    if (!currentState.reachable) {
                        sendCommand();
                    } else if (currentState.similarToState(desiredState)) {
                        state = CONTROL_PRCESS_STATE_COMPLETED;
                        reportResult();
                    } else {
                        sendCommand();
                    }
                } else {
                    state = CONTROL_PRCESS_STATE_FAILURE;
                    reportResult();
                }
            }
        });

    }

    public void toggleState(String id, final CozifyApiReal.CozifyCallback cbFinished) {
        state = CONTROL_PRCESS_STATE_CONTROL_LOCAL;
        this.cbFinished = cbFinished;
        if (startTime == null) {
            startTime = System.currentTimeMillis();
        }

        cozifyAPI.getSceneOrDeviceState(id, true, new CozifyApiReal.CozifyCallback() {
            public void result(boolean success, String status, JSONObject jsonResponse, JSONObject jsonRequest) {
                if (success) {
                    setCurrentStateFromJsonResponse(jsonResponse);
                    desiredState = new CozifySceneOrDeviceState();
                    desiredState.fromJson(jsonResponse);
                    desiredState.isOn = !currentState.isOn;
                    sendCommand();
                } else {
                    state = CONTROL_PRCESS_STATE_FAILURE;
                    reportResult();
                }
            }
        });
    }

    public void controlStateToDesired(String id, boolean desiredOnOffState, final CozifyApiReal.CozifyCallback cbFinished) {
        state = CONTROL_PRCESS_STATE_CONTROL_LOCAL;
        this.cbFinished = cbFinished;
        if (startTime == null) {
            startTime = System.currentTimeMillis();
        }
        desiredState = currentState;
        desiredState.isOn = desiredOnOffState;
        sendCommand();
    }

    public CozifySceneOrDeviceState getCurrentState() {
        return currentState;
    }

    private void setCurrentStateFromJsonResponse(JSONObject stateJson) {
        if (stateJson != null) {
            currentState = new CozifySceneOrDeviceState();
            currentState.fromJson(stateJson);
            saveState();
        }
    }

    public String getMeasurementAge() {
        long millis = 0;
        if (currentState != null){
            millis = System.currentTimeMillis() - currentState.timestamp;
        }
        return formatTimeStringSinceFromMs(millis);
    }

    public String formatTimeStringSinceFromMs(long millis) {
        long secs = millis / 1000;
        if (secs < 60) {
            return "";
        } else if (secs < 100) {
            return String.format("%d secs ago", secs);
        } else if (secs < 60*60*2) {
            return String.format("%d mins ago", secs/60);
        } else if (secs < 60*60*24*2) {
            return String.format("%d hours ago", secs/60/60);
        }
        return String.format("%d days ago", secs/60/60/24);
    }

    public String getMeasurementTimeStr() {
        if (currentState == null) return "";
        Calendar cal = Calendar.getInstance(Locale.ENGLISH);
        cal.setTimeInMillis(currentState.timestamp);
        Calendar now = Calendar.getInstance(Locale.ENGLISH);
        String dateStr = DateFormat.format("MM/dd HH:mm", cal).toString();
        if (now.get(Calendar.DATE) == cal.get(Calendar.DATE)) {
            dateStr = DateFormat.format("HH:mm", cal).toString();
        }
        return String.format("%s  (%d)",  dateStr, mAppWidgetId);
    }

    public String getMeasurementString(JSONArray selectedCapabilities) {
        String measurement = null;
        try {
            if (currentState != null && currentState.isSensor()) {
                List<String> wishlist = new ArrayList<String>();
                for (int i = 0; i < selectedCapabilities.length(); i++) {
                    wishlist.add(selectedCapabilities.getString(i));
                }
                measurement = currentState.getMeasurementsString(wishlist);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return measurement;
    }

    public boolean isOn() {
        if (currentState == null)
            return false;
        return currentState.isOn;
    }

    public boolean willBeOn() {
        if (currentState == null)
            return false;
        if (desiredState == null)
            return false;
        return desiredState.isOn;
    }

    public boolean isReachable() {
        if (currentState == null)
            return false;
        return currentState.reachable;
    }

    public void setReachable(boolean reachable) {
        if (currentState == null) return;
        currentState.reachable = reachable;
    }

}
