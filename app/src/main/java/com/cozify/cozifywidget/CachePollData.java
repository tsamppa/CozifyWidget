package com.cozify.cozifywidget;

import android.content.Context;

import org.json.JSONException;
import org.json.JSONObject;

public class CachePollData {
    public boolean init = false;
    private Context context;
    private String hubId = "A";

    private long lastPollTimestampSinceEpochMs = 0;
    private JSONObject lastPollDataJson = null;
    private boolean pollOngoing = false;

    public CachePollData(Context context, String hubId) {
        this.context = context.getApplicationContext();
        this.hubId = hubId;
        load();
    }

    private boolean load() {
        String json = PersistentStorage.getInstance(context).loadCachePollData(this.hubId);
        init = fromJsonString(json);
        return init;
    }

    private boolean save() {
        PersistentStorage.getInstance(context).saveCachePollData(hubId, toJsonString());
        init = true;
        return init;
    }

    public String toJsonString() {
        JSONObject json = new JSONObject();
        try {
            json.put("lastPollTimestampSinceEpochMs", lastPollTimestampSinceEpochMs);
            if (lastPollDataJson != null)
                json.put("lastPollDataJson", lastPollDataJson);
            json.put("pollOngoing", pollOngoing);

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json.toString();
    }

    public boolean fromJsonString(String jsonString) {
        if (jsonString == null) return false;
        try {
            JSONObject json = new JSONObject(jsonString);
            if (json.has("lastPollDataJson"))
                lastPollDataJson = json.getJSONObject("lastPollDataJson");
            if (json.has("lastPollTimestampSinceEpochMs"))
                lastPollTimestampSinceEpochMs = json.getLong("lastPollTimestampSinceEpochMs");
            if (json.has("pollOngoing"))
                pollOngoing = json.getBoolean("pollOngoing");
        } catch (JSONException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public JSONObject getLastPollDataJson() {
        load();
        return lastPollDataJson;
    }
    public boolean setLastPollDataJson(JSONObject lastPollDataJson) {
        this.lastPollDataJson = lastPollDataJson;
        lastPollTimestampSinceEpochMs = System.currentTimeMillis();
        return save();
    }

    public long getLastPollTimestamp() {
        load();
        return lastPollTimestampSinceEpochMs;
    }

    public boolean isPollOngoing() {
        load();
        return this.pollOngoing;
    }
    public void setPollOngoing(boolean pollOngoing) {
        this.pollOngoing = pollOngoing;
        save();
    }

}
