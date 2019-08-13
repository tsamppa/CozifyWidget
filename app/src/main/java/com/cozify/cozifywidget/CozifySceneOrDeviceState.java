package com.cozify.cozifywidget;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CozifySceneOrDeviceState {
    public boolean initialized = false;
    public long timestamp = 0;
    public String type = "";
    public String id = "";
    public String manufacturer = "";
    public String model = "";
    public String name = "";
    public String room = "";
    public double temperature = 0;
    public double humidity = 0;
    public double co2 = 0;
    public boolean reachable = true;
    public boolean isOn = false;
    public List<String> capabilities = new ArrayList<String>();

    public boolean fromJson(JSONObject source) {
        try {
            this.id = source.getString("id");
            this.timestamp = source.getLong("timestamp");
            this.type = source.getString("type");
            this.name = source.getString("name");
            this.isOn = source.getBoolean("isOn");
            if (source.has("capabilities")) {
                JSONArray cs = new JSONArray(source.getString("capabilities"));
                for (int i = 0; i < cs.length(); i++) {
                    String c = cs.getString(i);
                    capabilities.add(c);
                }
            }
            if (capabilities.contains("DEVICE")) {
                this.manufacturer = source.getString("manufacturer");
                this.model = source.getString("model");
                this.room = source.getString("room");
            }
            if (capabilities.contains("TEMPERATURE")) {
                this.temperature = source.getDouble("temperature");
            }
            if (capabilities.contains("HUMIDITY")) {
                this.humidity = source.getDouble("humidity");
            }
            if (capabilities.contains("CO2")) {
                this.co2 = source.getDouble("co2");
            }
            if (source.has("reachable")) {
                this.reachable = source.getBoolean("reachable");
            }
            initialized = true;
            return true;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean fromJsonStr(String source) {
        if (source != null) {
            try {
                JSONObject json = new JSONObject(source);
                return fromJson(json);
            } catch (JSONException e) {
                Log.e("Cozify Widget Error", "Parsing state failed from json string:" +source);
                e.printStackTrace();
            }
        }
        return false;
    }

    public void fromPollData(JSONObject pollData, long timestamp) {

        try {
            type = pollData.getString("type");
            this.timestamp = timestamp;
            this.id = pollData.getString("id");
            this.name = pollData.getString("name");
            if (pollData.has("capabilities")) {
                JSONObject caps = pollData.getJSONObject("capabilities");
                JSONArray cs = caps.getJSONArray("values");
                for (int i = 0; i < cs.length(); i++) {
                    String c = cs.getString(i);
                    capabilities.add(c);
                }
            }
            if (pollData.has("state")) {
                if (pollData.getJSONObject("state").has("reachable")) {
                    this.reachable = pollData.getJSONObject("state").getBoolean("reachable");
                }

                if (capabilities.contains("ON_OFF")) {
                    this.isOn = pollData.getJSONObject("state").getBoolean("isOn");
                }
                if (capabilities.contains("TEMPERATURE")) {
                    temperature = pollData.getJSONObject("state").getDouble("temperature");
                }
                if (capabilities.contains("HUMIDITY")) {
                    humidity = pollData.getJSONObject("state").getDouble("humidity");
                }
                if (capabilities.contains("CO2")) {
                    co2 = pollData.getJSONObject("state").getDouble("co2Ppm");
                }
            }
            if (pollData.has("room")) {
                this.room = pollData.getString("room");
            }
            if (pollData.has("manufacturer")) {
                this.manufacturer = pollData.getString("manufacturer");
            }
            if (pollData.has("model")) {
                this.model = pollData.getString("model");
            }
            if (isScene()) {
                this.isOn = pollData.getBoolean("isOn");
            }
            initialized = true;
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
        jsonObject.put("temperature", this.temperature);
        jsonObject.put("humidity", this.humidity);
        jsonObject.put("co2", this.co2);
        jsonObject.put("capabilities", this.capabilities.toString());
        return jsonObject;
    }
    public boolean isDevice() {
        return capabilities.contains("DEVICE");
    }
    public boolean isScene() {
        return type.contains("SCENE");
    }
    public boolean hasMeasurement() {
       return capabilities.contains("TEMPERATURE") || capabilities.contains("HUMIDITY");
    }
    public boolean isOnOff() {
        return capabilities.contains("ON_OFF") || isScene();
    }

    public boolean similarToState(CozifySceneOrDeviceState other) {
        if (other == null) return false;
        return other.isOn == this.isOn;
    }

    public CozifyCommand getCommandTowardsDesiredState(CozifySceneOrDeviceState desiredState) {
        if (desiredState == null) throw new NullPointerException("desiredState is null in method getCommandTowardsDesiredState(CozifySceneOrDeviceState desiredState)");
        String path = isScene() ? "/scenes/command" : "/devices/command";
        String cmd = isScene() ? "CMD_SCENE" : "CMD_DEVICE";
        String commandString = desiredState.isOn ? cmd + "_ON" : cmd + "_OFF";
        return new CozifyCommand(path, commandString);
    }

    public String getMeasurementsString() {
        String measurement = null;
        if (capabilities.contains("CO2")) {
            measurement = String.format(Locale.ENGLISH, "%.0f", co2);
        }
        if (capabilities.contains("HUMIDITY")) {
            if (measurement != null) {
                measurement += "\n";
            }
            measurement += "\n" + String.format(Locale.ENGLISH, "%.0f %%", humidity);
        }
        if (capabilities.contains("TEMPERATURE")) {
            if (measurement != null) {
                measurement += "\n";
            }
            measurement += String.format(Locale.ENGLISH, "%.1f C", temperature);
        }
        return measurement;
    }
}
