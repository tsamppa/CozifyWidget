package com.cozify.cozifywidget;

import android.util.Base64;

import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;

public class CozifyCloudToken {
    static public String parseHubNameFromToken(String token) {
        String hubName = "";
        try {
            JSONObject json = new JSONObject(getDecodedJwt(token));
            hubName = json.getString("hub_name");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return hubName;
    }

    static public String getDecodedJwt(String jwt) {
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
            throw new RuntimeException("Couldnt decode jwt", e);
        }
        return result1;
    }
}
