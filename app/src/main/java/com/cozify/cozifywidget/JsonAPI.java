package com.cozify.cozifywidget;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;


public class JsonAPI {

    public interface JsonCallback {
        void onResponse(int statusCode, JSONObject json);
    }

    public interface StringCallback {
        void onResponse(int statusCode, String result);
    }

    private HashMap<String, String> headers = new HashMap<>();

    public JsonAPI() {
        headers.put("Content-Type","application/json");
        headers.put("Accept","application/json");
    }

    public void addHeader(String name, String value) {
        headers.put(name, value);
    }
    public void removeHeader(String name) {
        headers.remove(name);
    }

    public void get(String url, final JsonCallback callback) {
        if (callback == null)
            throw new NullPointerException();
        com.cozify.cozifywidget.HttpRequest r = new com.cozify.cozifywidget.HttpRequest(com.cozify.cozifywidget.HttpRequest.Method.GET, url);
        headers.put("Content-Type","text/plain");
        headers.put("Accept","application/json");
        r.setHeaders(headers);
        r.setCallback(callbackForJsonCallback(callback));
        new com.cozify.cozifywidget.HttpTask().execute(r);
    }

    public void string_get(String url, final StringCallback callback) {
        com.cozify.cozifywidget.HttpRequest r = new com.cozify.cozifywidget.HttpRequest(com.cozify.cozifywidget.HttpRequest.Method.GET, url);
        headers.put("Content-Type","text/plain");
        headers.put("Accept","text/plain");
        r.setHeaders(headers);
        r.setCallback(callbackForStringCallback(callback));
        new com.cozify.cozifywidget.HttpTask().execute(r);
    }


    public void post(String url, JSONObject postData, JsonCallback callback) {
        if (callback == null)
            throw new NullPointerException();
        com.cozify.cozifywidget.HttpRequest r = new com.cozify.cozifywidget.HttpRequest(com.cozify.cozifywidget.HttpRequest.Method.POST, url);
        headers.put("Content-Type","application/json");
        headers.put("Accept","application/json");
        r.setHeaders(headers);
        r.setCallback(callbackForJsonCallback(callback));
        r.setPostData(postData.toString());
        new com.cozify.cozifywidget.HttpTask().execute(r);
    }

    public void post(String url, JSONObject postData, StringCallback callback) {
        if (callback == null)
            throw new NullPointerException();
        com.cozify.cozifywidget.HttpRequest r = new com.cozify.cozifywidget.HttpRequest(com.cozify.cozifywidget.HttpRequest.Method.POST, url);
        headers.put("Content-Type","application/json");
        headers.put("Accept", "text/plain");
        r.setHeaders(headers);
        r.setCallback(callbackForStringCallback(callback));
        r.setPostData(postData.toString());
        new com.cozify.cozifywidget.HttpTask().execute(r);
    }

    public void post(String url, String postData, StringCallback callback) {
        com.cozify.cozifywidget.HttpRequest r = new com.cozify.cozifywidget.HttpRequest(com.cozify.cozifywidget.HttpRequest.Method.POST, url);
        headers.put("Content-Type", "text/plain");
        headers.put("Accept", "text/plain");
        r.setHeaders(headers);
        r.setCallback(callbackForStringCallback(callback));
        if (postData.length() > 0) r.setPostData(postData);
        new com.cozify.cozifywidget.HttpTask().execute(r);
    }

    public void put(String url, JSONObject data, final JsonCallback callback) {
        com.cozify.cozifywidget.HttpRequest r = new com.cozify.cozifywidget.HttpRequest(com.cozify.cozifywidget.HttpRequest.Method.PUT, url);
        headers.put("Content-Type","application/json");
        headers.put("Accept","application/json");
        r.setHeaders(headers);
        r.setCallback(callbackForJsonCallback(callback));
        r.setPostData(data.toString());
        new com.cozify.cozifywidget.HttpTask().execute(r);
    }

    public void put(String url, JSONObject data, final StringCallback callback) {
        com.cozify.cozifywidget.HttpRequest r = new com.cozify.cozifywidget.HttpRequest(com.cozify.cozifywidget.HttpRequest.Method.PUT, url);
        headers.put("Content-Type","application/json");
        headers.put("Accept","text/plain");
        r.setHeaders(headers);
        r.setCallback(callbackForStringCallback(callback));
        r.setPostData(data.toString());
        new com.cozify.cozifywidget.HttpTask().execute(r);
    }

    public void put(String url, JSONArray data, final StringCallback callback) {
        com.cozify.cozifywidget.HttpRequest r = new com.cozify.cozifywidget.HttpRequest(com.cozify.cozifywidget.HttpRequest.Method.PUT, url);
        headers.put("Content-Type","application/json");
        headers.put("Accept","text/plain");
        r.setHeaders(headers);
        r.setCallback(callbackForStringCallback(callback));
        r.setPostData(data.toString());
        new com.cozify.cozifywidget.HttpTask().execute(r);
    }

    public void put(String url, String data, final StringCallback callback) {
        com.cozify.cozifywidget.HttpRequest r = new com.cozify.cozifywidget.HttpRequest(com.cozify.cozifywidget.HttpRequest.Method.PUT, url);
        headers.put("Content-Type","text/plain");
        headers.put("Accept","text/plain");
        r.setHeaders(headers);
        r.setCallback(callbackForStringCallback(callback));
        r.setPostData(data);
        new com.cozify.cozifywidget.HttpTask().execute(r);
    }

    public void put(String url, String data, final JsonCallback callback) {
        com.cozify.cozifywidget.HttpRequest r = new com.cozify.cozifywidget.HttpRequest(com.cozify.cozifywidget.HttpRequest.Method.PUT, url);
        headers.put("Content-Type","application/json");
        headers.put("Accept","application/json");
        r.setHeaders(headers);
        r.setCallback(callbackForJsonCallback(callback));
        r.setPostData(data);
        new com.cozify.cozifywidget.HttpTask().execute(r);
    }


    private com.cozify.cozifywidget.HttpRequest.RequestCallback callbackForJsonCallback(final JsonCallback jsonCallback) {
        return new com.cozify.cozifywidget.HttpRequest.RequestCallback() {
            @Override
            public void onResponse(com.cozify.cozifywidget.HttpResponse r) {
                JSONObject json = null;
                try {
                    json = new JSONObject(r.getResponse());
                } catch (JSONException e) {
                    try {
                        json = new JSONObject();
                        json.put("response", r.getResponse());
                    } catch (JSONException e2) {
                        e2.printStackTrace();
                    }
                }
                int code = r.getResponseCode();
                if (200 != code) {
                    try {
                        json.put("errorCode", code);
                    } catch (JSONException e2) {
                        Log.d("JSONException while putting HTTP ERROR CODE",e2.getMessage() + " code: "+ code);
                    }
                }
                jsonCallback.onResponse(code, json);
            }
        };
    }

    private com.cozify.cozifywidget.HttpRequest.RequestCallback callbackForStringCallback(final StringCallback stringCallback) {
        return new com.cozify.cozifywidget.HttpRequest.RequestCallback() {
            @Override
            public void onResponse(com.cozify.cozifywidget.HttpResponse r) {
                stringCallback.onResponse(r.getResponseCode(), r.getResponse());
            }
        };
    }
}