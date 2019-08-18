package com.cozify.cozifywidget;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

import androidx.annotation.IntDef;

import org.json.JSONObject;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class CozifySceneOrDeviceStateManager implements Runnable {
    private CozifySceneOrDeviceState currentState = null;
    private CozifySceneOrDeviceState desiredState;
    private Handler handler;
    private Long startTime = null;
    private CozifyApiReal.CozifyCallback cbFinished = null;

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


    private static final int retryDelay = 30 * 1000; // 30 secs
    private static final int maxRuntime = 10*60*1000; // 10 minutes
    private int mAppWidgetId;
    private Context mContext;

    private CozifyApiReal cozifyAPI = new CozifyApiReal();

    @ControlProcessState private int state = CONTROL_PRCESS_STATE_INIT;

    CozifySceneOrDeviceStateManager(Context context, int appWidgetId) {
        handler = new Handler();
        mAppWidgetId = appWidgetId;
        mContext = context;
    }

    public void setCloudToken(String cloudToken) {
        cozifyAPI.setCloudToken(cloudToken);
    }

    public void setHubKey(String hubKey) {
        cozifyAPI.setHubKey(hubKey);
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

    public void updateCurrentState(String device_id, final CozifyApiReal.CozifyCallback cb) {
        cozifyAPI.getSceneOrDeviceState(device_id, new CozifyApiReal.CozifyCallback() {
            public void result(boolean success, String status, JSONObject jsonResponse, JSONObject jsonRequest) {
                if (success) {
                    setCurrentStateFromJsonResponse(jsonResponse);
                }
                cb.result(success, status, jsonResponse, jsonRequest);
            }
        });
    }

    private void updateCurrentStateAndRetryControlAfterDelay() {
        cozifyAPI.getSceneOrDeviceState(desiredState.id, new CozifyApiReal.CozifyCallback() {
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
        cozifyAPI.getSceneOrDeviceState(currentState.id, new CozifyApiReal.CozifyCallback() {
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

        cozifyAPI.getSceneOrDeviceState(id, new CozifyApiReal.CozifyCallback() {
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


    public CozifySceneOrDeviceState getCurrentState() {
        return currentState;
    }

    public void setCurrentState(CozifySceneOrDeviceState state) {
        if (currentState == null) {
            currentState = state;
        } else if (state.timestamp > currentState.timestamp) {
            currentState = state;
        }
    }

    private void setCurrentStateFromJsonResponse(JSONObject stateJson) {
        if (stateJson != null) {
            CozifySceneOrDeviceState state = new CozifySceneOrDeviceState();
            state.fromJson(stateJson);
            setCurrentState(state);
        }
    }

    public String getMeasurementString() {
        String measurement = null;
        if (currentState != null && currentState.hasMeasurement()) {
            measurement = currentState.getMeasurementsString();
        }
        return measurement;
    }

    public boolean isOn() {
        if (currentState == null)
            return false;
        return currentState.isOn;
    }

    public boolean isReachable() {
        if (currentState == null)
            return false;
        return currentState.reachable;
    }

    public void setReachable(boolean reachable) {
        currentState.reachable = reachable;
    }

}
