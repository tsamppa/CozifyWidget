package com.example.cozifywidget;

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
        com.example.cozifywidget.HttpRequest r = new com.example.cozifywidget.HttpRequest(com.example.cozifywidget.HttpRequest.Method.GET, url);
        headers.put("Content-Type","text/plain");
        headers.put("Accept","application/json");
        r.setHeaders(headers);
        r.setCallback(callbackForJsonCallback(callback));
        new com.example.cozifywidget.HttpTask().execute(r);
    }

    public void string_get(String url, final StringCallback callback) {
        com.example.cozifywidget.HttpRequest r = new com.example.cozifywidget.HttpRequest(com.example.cozifywidget.HttpRequest.Method.GET, url);
        headers.put("Content-Type","text/plain");
        headers.put("Accept","text/plain");
        r.setHeaders(headers);
        r.setCallback(callbackForStringCallback(callback));
        new com.example.cozifywidget.HttpTask().execute(r);
    }


    public void post(String url, JSONObject postData, JsonCallback callback) {
        if (callback == null)
            throw new NullPointerException();
        com.example.cozifywidget.HttpRequest r = new com.example.cozifywidget.HttpRequest(com.example.cozifywidget.HttpRequest.Method.POST, url);
        headers.put("Content-Type","application/json");
        headers.put("Accept","application/json");
        r.setHeaders(headers);
        r.setCallback(callbackForJsonCallback(callback));
        r.setPostData(postData.toString());
        new com.example.cozifywidget.HttpTask().execute(r);
    }

    public void post(String url, JSONObject postData, StringCallback callback) {
        if (callback == null)
            throw new NullPointerException();
        com.example.cozifywidget.HttpRequest r = new com.example.cozifywidget.HttpRequest(com.example.cozifywidget.HttpRequest.Method.POST, url);
        headers.put("Content-Type","application/json");
        headers.put("Accept", "text/plain");
        r.setHeaders(headers);
        r.setCallback(callbackForStringCallback(callback));
        r.setPostData(postData.toString());
        new com.example.cozifywidget.HttpTask().execute(r);
    }

    public void post(String url, String postData, StringCallback callback) {
        com.example.cozifywidget.HttpRequest r = new com.example.cozifywidget.HttpRequest(com.example.cozifywidget.HttpRequest.Method.POST, url);
        headers.put("Content-Type", "text/plain");
        headers.put("Accept", "text/plain");
        r.setHeaders(headers);
        r.setCallback(callbackForStringCallback(callback));
        if (postData.length() > 0) r.setPostData(postData);
        new com.example.cozifywidget.HttpTask().execute(r);
    }

    public void put(String url, JSONObject data, final JsonCallback callback) {
        com.example.cozifywidget.HttpRequest r = new com.example.cozifywidget.HttpRequest(com.example.cozifywidget.HttpRequest.Method.PUT, url);
        headers.put("Content-Type","application/json");
        headers.put("Accept","application/json");
        r.setHeaders(headers);
        r.setCallback(callbackForJsonCallback(callback));
        r.setPostData(data.toString());
        new com.example.cozifywidget.HttpTask().execute(r);
    }

    public void put(String url, JSONObject data, final StringCallback callback) {
        com.example.cozifywidget.HttpRequest r = new com.example.cozifywidget.HttpRequest(com.example.cozifywidget.HttpRequest.Method.PUT, url);
        headers.put("Content-Type","application/json");
        headers.put("Accept","text/plain");
        r.setHeaders(headers);
        r.setCallback(callbackForStringCallback(callback));
        r.setPostData(data.toString());
        new com.example.cozifywidget.HttpTask().execute(r);
    }

    public void put(String url, String data, final StringCallback callback) {
        com.example.cozifywidget.HttpRequest r = new com.example.cozifywidget.HttpRequest(com.example.cozifywidget.HttpRequest.Method.PUT, url);
        headers.put("Content-Type","text/plain");
        headers.put("Accept","text/plain");
        r.setHeaders(headers);
        r.setCallback(callbackForStringCallback(callback));
        r.setPostData(data);
        new com.example.cozifywidget.HttpTask().execute(r);
    }

    public void put(String url, String data, final JsonCallback callback) {
        com.example.cozifywidget.HttpRequest r = new com.example.cozifywidget.HttpRequest(com.example.cozifywidget.HttpRequest.Method.PUT, url);
        headers.put("Content-Type","application/json"); // TODO
        headers.put("Accept","application/json");
        r.setHeaders(headers);
        r.setCallback(callbackForJsonCallback(callback));
        r.setPostData(data);
        new com.example.cozifywidget.HttpTask().execute(r);
    }


    private com.example.cozifywidget.HttpRequest.RequestCallback callbackForJsonCallback(final JsonCallback jsonCallback) {
        return new com.example.cozifywidget.HttpRequest.RequestCallback() {
            @Override
            public void onResponse(com.example.cozifywidget.HttpResponse r) {
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
                jsonCallback.onResponse(r.getResponseCode(), json);
            }
        };
    }

    private com.example.cozifywidget.HttpRequest.RequestCallback callbackForStringCallback(final StringCallback stringCallback) {
        return new com.example.cozifywidget.HttpRequest.RequestCallback() {
            @Override
            public void onResponse(com.example.cozifywidget.HttpResponse r) {
                stringCallback.onResponse(r.getResponseCode(), r.getResponse());
            }
        };
    }
}