package com.example.cozifywidget;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class CozifySceneOrDeviceState {
    public long timestamp = 0;
    public String type = "";
    public String id = "";
    public String manufacturer = "";
    public String model = "";
    public String name = "";
    public String room = "";
    public boolean reachable = true;
    public boolean isOn = false;
    public List<String> capabilities = new ArrayList<String>();

    public void fromJson(JSONObject source) {
        try {
            this.id = source.getString("id");
            this.timestamp = source.getLong("timestamp");
            this.type = source.getString("type");
            this.name = source.getString("name");
            this.isOn = source.getBoolean("isOn");
            if (type.contains("DEVICE")) {
                this.manufacturer = source.getString("manufacturer");
                this.model = source.getString("model");
                this.room = source.getString("room");
                this.reachable = source.getBoolean("reachable");
                JSONArray cs = source.getJSONArray("capabilities");
                for (int i = 0; i < cs.length(); i++) {
                    String c = cs.getString(i);
                    capabilities.add(c);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    public void fromPollData(JSONObject pollData, long timestamp) {
        try {
            type = pollData.getString("type");
            this.timestamp = timestamp;
            this.id = pollData.getString("id");
            this.name = pollData.getString("name");
            if (isDevice()) {
                JSONObject caps = pollData.getJSONObject("capabilities");
                JSONArray cs = caps.getJSONArray("values");
                for (int i = 0; i < cs.length(); i++) {
                    String c = cs.getString(i);
                    capabilities.add(c);
                }
                if (capabilities.contains("ON_OFF")) {
                    this.isOn = pollData.getJSONObject("state").getBoolean("isOn");
                }
                this.room = pollData.getString("room");
                this.manufacturer = pollData.getString("manufacturer");
                this.model = pollData.getString("model");
                this.reachable = pollData.getJSONObject("state").getBoolean("reachable");
            }
            if (isScene()) {
                this.isOn = pollData.getBoolean("isOn");
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public JSONObject toJson() throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("type", this.type);
        jsonObject.put("id", this.id);
        jsonObject.put("timestamp", this.timestamp);
        jsonObject.put("manufacturer", this.manufacturer);
        jsonObject.put("model", this.model);
        jsonObject.put("name", this.name);
        jsonObject.put("room", this.room);
        jsonObject.put("reachable", this.reachable);
        jsonObject.put("isOn", this.isOn);
        jsonObject.put("capabilities", this.capabilities.toString());
        return jsonObject;
    }
    public boolean isDevice() {
        return !type.contains("SCENE");
    }
    public boolean isScene() {
        return type.contains("SCENE");
    }

    public boolean similarToState(CozifySceneOrDeviceState other) {
        if (other == null) return false;
        return other.isOn == this.isOn;
    }

    public CozifyCommand getCommandTowardsDesiredState(CozifySceneOrDeviceState desiredState) {
        if (desiredState == null) throw new NullPointerException("desiredState is null in method getCommandTowardsDesiredState(CozifySceneOrDeviceState desiredState)");
        String commandString = desiredState.isOn ? "CMD_" + this.type + "_ON" : "CMD_" + this.type + "_OFF";
        String path = this.type.contains("SCENE") ? "/scenes/command" : "/devices/command";
        return new CozifyCommand(path, commandString);
    }
}
