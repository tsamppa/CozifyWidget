package com.example.cozifywidget;

import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;

import javax.net.ssl.HttpsURLConnection;


public class HttpTask extends AsyncTask<com.example.cozifywidget.HttpRequest, String, com.example.cozifywidget.HttpResponse> {

    @Override
    protected com.example.cozifywidget.HttpResponse doInBackground(com.example.cozifywidget.HttpRequest... params) {
        URL url;
        com.example.cozifywidget.HttpRequest request = params[0];
        if (request == null) {
            Log.e("HttpTask", "BAD HttpRequest null");
            throw new NullPointerException();
        }
        com.example.cozifywidget.HttpResponse response = new com.example.cozifywidget.HttpResponse(-1, "N/A", request.getCallback());
        try {
            if (request.getURL() == null || request.getMethod() == null || request.getCallback() == null) {
                Log.e("HttpTask", "BAD HttpRequest");
                throw new Exception();
            }
            url = new URL(request.getURL());
            if (url.getProtocol().equals("https")) {
                response = httpsRequest(url, request);
            } else {
                response = httpRequest(url, request);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return response;
    }

    private com.example.cozifywidget.HttpResponse  httpsRequest(URL url, com.example.cozifywidget.HttpRequest request) {
        com.example.cozifywidget.HttpResponse response = new com.example.cozifywidget.HttpResponse(-1, "N/A", request.getCallback());
        HttpsURLConnection urlConnectionHttps = null;
        try {
            urlConnectionHttps = (HttpsURLConnection) url.openConnection();
            Log.v("HttpTask", request.getMethodString());

            urlConnectionHttps.setRequestMethod(request.getMethodString());

            if (request.getHeaders() != null) {
                for (HashMap.Entry<String, String> pair : request.getHeaders().entrySet()) {
                    urlConnectionHttps.setRequestProperty(pair.getKey(), pair.getValue());
                }
            }
            urlConnectionHttps.setConnectTimeout(100000);
            urlConnectionHttps.setReadTimeout(100000);
            if (request.getPostData() != null) {
                urlConnectionHttps.setDoOutput(true);
                DataOutputStream wr = new DataOutputStream(urlConnectionHttps.getOutputStream());
                byte[] postData = request.getPostData().getBytes();
                wr.write(postData);
            }
            urlConnectionHttps.connect();

            int responseCode = urlConnectionHttps.getResponseCode();
            String responseString;
            if (responseCode == HttpsURLConnection.HTTP_OK) {
                responseString = readStream(urlConnectionHttps.getInputStream());
            } else {
                responseString = readStream(urlConnectionHttps.getErrorStream());
            }
            Log.v("HttpTask", "Response code:" + responseCode);
            Log.v("HttpTask", responseString);
            response = new com.example.cozifywidget.HttpResponse(responseCode, responseString, request.getCallback());

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (urlConnectionHttps != null)
                urlConnectionHttps.disconnect();
        }
        return response;
    }

    private com.example.cozifywidget.HttpResponse  httpRequest(URL url, com.example.cozifywidget.HttpRequest request) {
        com.example.cozifywidget.HttpResponse response = new com.example.cozifywidget.HttpResponse(-1, "N/A", request.getCallback());
        HttpURLConnection urlConnectionHttp = null;
        try {
            urlConnectionHttp = (HttpURLConnection) url.openConnection();
            Log.v("HttpTask", request.getMethodString());

            urlConnectionHttp.setRequestMethod(request.getMethodString());

            if (request.getHeaders() != null) {
                for (HashMap.Entry<String, String> pair : request.getHeaders().entrySet()) {
                    urlConnectionHttp.setRequestProperty(pair.getKey(), pair.getValue());
                }
            }
            urlConnectionHttp.setConnectTimeout(100000);
            urlConnectionHttp.setReadTimeout(100000);
            if (request.getPostData() != null) {
                urlConnectionHttp.setDoOutput(true);
                DataOutputStream wr = new DataOutputStream(urlConnectionHttp.getOutputStream());
                byte[] postData = request.getPostData().getBytes();
                wr.write(postData);
            }
            urlConnectionHttp.connect();

            int responseCode = urlConnectionHttp.getResponseCode();
            String responseString;
            if (responseCode == HttpsURLConnection.HTTP_OK) {
                responseString = readStream(urlConnectionHttp.getInputStream());
            } else {
                responseString = readStream(urlConnectionHttp.getErrorStream());
            }
            Log.v("HttpTask", "Response code:" + responseCode);
            Log.v("HttpTask", responseString);
            response = new com.example.cozifywidget.HttpResponse(responseCode, responseString, request.getCallback());

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (urlConnectionHttp != null)
                urlConnectionHttp.disconnect();
        }
        return response;
    }



    private String readStream(InputStream in) {
        BufferedReader reader = null;
        StringBuffer response = new StringBuffer();
        try {
            reader = new BufferedReader(new InputStreamReader(in));
            String line = "";
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return response.toString();
    }

    @Override
    protected void onPostExecute(com.example.cozifywidget.HttpResponse response) {
        super.onPostExecute(response);
        HttpRequest.RequestCallback callback = response.getCallback();
        if (callback != null) {
            callback.onResponse(response);
        } else {
            throw new NullPointerException();
        }
    }
}