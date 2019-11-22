package com.cozify.cozifywidget;

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


public class HttpTask extends AsyncTask<com.cozify.cozifywidget.HttpRequest, String, com.cozify.cozifywidget.HttpResponse> {

    private boolean logRequests = true;

    @Override
    protected com.cozify.cozifywidget.HttpResponse doInBackground(com.cozify.cozifywidget.HttpRequest... params) {
        URL url;
        com.cozify.cozifywidget.HttpRequest request = params[0];
        if (request == null) {
            Log.e("HttpTask", "BAD HttpRequest null");
            throw new NullPointerException();
        }
        com.cozify.cozifywidget.HttpResponse response = new com.cozify.cozifywidget.HttpResponse(-1, "N/A", request.getCallback());
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
            response.setResponse(e.getMessage());
        }

        return response;
    }

    private com.cozify.cozifywidget.HttpResponse httpsRequest(URL url, com.cozify.cozifywidget.HttpRequest request) {
        com.cozify.cozifywidget.HttpResponse response = new com.cozify.cozifywidget.HttpResponse(-1, "N/A", request.getCallback());
        HttpsURLConnection urlConnectionHttps = null;
        try {
            urlConnectionHttps = (HttpsURLConnection) url.openConnection();
            if (logRequests) Log.i("HttpsTask", request.getMethodString() + " " + url.toString());
            if (logRequests) Log.i("HttpsTaskDetails",  " POST DATA: " + request.getPostData() + " HEADERS: " + request.getHeaders().toString());

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
            if (logRequests) Log.i("HttpsTask", "Response code:" + responseCode);
            if (logRequests) Log.i("HttpsTask", responseString);
            response.setResponse(responseString);
            response.setResponseCode(responseCode);

        } catch (Exception e) {
            e.printStackTrace();
            response.setResponse(e.getMessage());
            response.setResponseCode(createCodeForExceptionMessage(e.getMessage()));
        } finally {
            if (urlConnectionHttps != null)
                urlConnectionHttps.disconnect();
        }
        if (logRequests) Log.i("HttpTaskResponse", response.getResponse() + "(" + response.getResponseCode() + ")");
        return response;
    }

    private com.cozify.cozifywidget.HttpResponse httpRequest(URL url, com.cozify.cozifywidget.HttpRequest request) {
        com.cozify.cozifywidget.HttpResponse response = new com.cozify.cozifywidget.HttpResponse(-1, "N/A", request.getCallback());
        HttpURLConnection urlConnectionHttp = null;
        try {
            urlConnectionHttp = (HttpURLConnection) url.openConnection();
            if (logRequests) Log.i("HttpTask", request.getMethodString() + " " + url.toString());
            if (logRequests) Log.i("HttpTaskDetails",  " POST DATA: " + request.getPostData() + " HEADERS: " + request.getHeaders().toString());

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
            if (logRequests) Log.i("HttpTask", "Response code:" + responseCode);
            if (logRequests) Log.i("HttpTask", responseString);
            response.setResponse(responseString);
            response.setResponseCode(responseCode);

        } catch (Exception e) {
            e.printStackTrace();
            response.setResponse(e.getMessage());
            response.setResponseCode(createCodeForExceptionMessage(e.getMessage()));
        } finally {
            if (urlConnectionHttp != null)
                urlConnectionHttp.disconnect();
        }
        if (logRequests) Log.i("HttpTaskResponse", response.getResponse() + "(" + response.getResponseCode() + ")");
        return response;
    }

    private int createCodeForExceptionMessage(String message) {
        int code = -1;
        if (message != null) {
            if (message.equals("Connection refused"))
                code = 401;
            if (message.equals("Authentication failed"))
                code = 401;
        }
        return code;
    }

    private String readStream(InputStream in) {
        BufferedReader reader = null;
        StringBuffer response = new StringBuffer();
        try {
            reader = new BufferedReader(new InputStreamReader(in));
            String line;
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
    protected void onPostExecute(com.cozify.cozifywidget.HttpResponse response) {
        super.onPostExecute(response);
        HttpRequest.RequestCallback callback = response.getCallback();
        if (callback != null) {
            callback.onResponse(response);
        } else {
            throw new NullPointerException();
        }
    }
}