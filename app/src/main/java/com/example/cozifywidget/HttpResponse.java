package com.example.cozifywidget;

public class HttpResponse {
    private int responseCode;
    private String response;
    private HttpRequest.RequestCallback callback;

    public HttpResponse(int responseCode, String response, HttpRequest.RequestCallback callback) {
        this.responseCode = responseCode;
        this.response = response;
        if (callback == null)
            throw new NullPointerException();
        this.callback = callback;
    }

    public HttpResponse() {

    }

    public int getResponseCode() {
        return responseCode;
    }

    public void setResponseCode(int responseCode) {
        this.responseCode = responseCode;
    }

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }

    public HttpRequest.RequestCallback getCallback() {
        if (callback == null)
            throw new NullPointerException();
        return callback;
    }

    public void setCallback(HttpRequest.RequestCallback callback) {
        this.callback = callback;
    }
}