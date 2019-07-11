package com.example.cozifywidget;

import android.os.Handler;

import org.json.JSONObject;

public class CozifySceneOrDeviceStateManager implements Runnable {
    private CozifySceneOrDeviceState currentState = null;
    private CozifySceneOrDeviceState desiredState;
    private Handler handler;
    private Long startTime = null;
    private CozifyAPI.CozifyCallback cbFinished = null;

    private static final int INIT = 0;
    private static final int RUNNING = 1;
    private static final int COMPLETED = 2;
    private static final int ABORTED = 3;
    private static final int EXPIRED = 4;

    private static final int retryDelay = 30 * 1000; // 30 secs
    private static final int maxRuntime = 10*60*1000; // 10 minutes

    private static CozifyAPI cozifyAPI = CozifyApiReal.getInstance();

    private int state = INIT;

    CozifySceneOrDeviceStateManager(CozifySceneOrDeviceState desiredState, final CozifyAPI.CozifyCallback cbFinished) {
        handler = new Handler();
        this.desiredState = desiredState;
        this.cbFinished = cbFinished;
    }

    public void run() {
        state = RUNNING;
        if (startTime == null) {
            startTime = System.currentTimeMillis();
        }
        try {
            if (System.currentTimeMillis() - startTime > maxRuntime) {
                state = EXPIRED;
                handler.removeCallbacks(this);
                reportResult();
            } else if (sendCommand()) {
                handler.removeCallbacks(this);
                reportResult();
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void reportResult() {
        if (cbFinished != null) {
            switch (state) {
                case INIT: {
                    cbFinished.result(false, "INIT", null, null);
                    break;
                }
                case RUNNING: {
                    cbFinished.result(false, "RUNNING", null, null);
                    break;
                }
                case ABORTED: {
                    cbFinished.result(false, "ABORTED", null, null);
                    break;
                }
                case COMPLETED: {
                    cbFinished.result(true, "COMPLETED", null, null);
                    break;
                }
                case EXPIRED: {
                    cbFinished.result(false, "EXPIRED", null, null);
                    break;
                }
            }
        }
        cbFinished = null;
    }

    public void abort() {
        state = ABORTED;
        handler.removeCallbacks(this);
        reportResult();
    }

    private boolean checkIfCompleted() {
        boolean completed = desiredState.similarToState(currentState);
        if (completed) {
            state = COMPLETED;
            reportResult();
        }
        return false;
    }

    private void updateCurrentState(final boolean delayRetry) {
        cozifyAPI.getDeviceState(desiredState.id, new CozifyAPI.CozifyCallback() {
            public void result(boolean success, String status, JSONObject jsonResponse, JSONObject jsonRequest) {
                if (success) {
                    currentState = new CozifySceneOrDeviceState();
                    currentState.fromJson(jsonResponse);
                    if (checkIfCompleted()) return;
                }
                retry(delayRetry);
            }
        });
    }

    private void retry(boolean delayRetry) {
        if (delayRetry) {
            handler.postDelayed(this, retryDelay);
        } else {
            run();
        }
    }

    // Returns true if desired state reached
    private boolean sendCommand() {
        // resend command
        cozifyAPI.sendCommand(currentState.id, currentState.getCommandTowardsDesiredState(desiredState),
                new CozifyAPI.CozifyCallback() {
                    @Override
                    public void result(boolean success, String status, JSONObject jsonResponse, JSONObject jsonRequest) {
                        updateCurrentState(true);
                    }
                });
        return false; // Not yet ready
    }

    public void commence(CozifySceneOrDeviceState desiredState) {
        this.desiredState = desiredState;
        updateCurrentState(false);
    }

}
