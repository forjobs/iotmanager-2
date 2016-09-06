package com.naumchevski.iotcontrol.util;


import android.support.annotation.NonNull;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;

public final class HTTPUtil {

    private final static String ENCODING = "UTF-8";

    public static InputStream getChannelInfo(String u) throws IOException {
        InputStream is = null;

        URL url = new URL(u);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setFixedLengthStreamingMode(3);
        is = conn.getInputStream();
        return is;
    }

    public static InputStream getChannel(String u) throws IOException {
        InputStream is = null;

        URL url = new URL(u);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setReadTimeout(10000);
        conn.setConnectTimeout(15000);
        conn.setRequestMethod("GET");
        conn.setDoInput(true);
        conn.connect();

        int response = conn.getResponseCode();
        Log.d("HTTP", "The response is: " + response);
        is = conn.getInputStream();
        return is;
    }

    public static InputStream createChannel(String u, String name) throws IOException {
        HttpURLConnection conn = null;
        InputStream is = null;

        URL url = new URL(u);
        conn = (HttpURLConnection) url.openConnection();
        conn.setReadTimeout(10000);
        conn.setConnectTimeout(15000);
        conn.setRequestMethod("POST");
        conn.setDoInput(true);
        conn.setDoOutput(true);

        List<AbstractMap.SimpleEntry<String, String>> params =
                new ArrayList<AbstractMap.SimpleEntry<String, String>>();
        params.add(new AbstractMap.SimpleEntry<String, String>("name", name));

        OutputStream os = conn.getOutputStream();
        BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(os, ENCODING));
        writer.write(getQuery(params));
        writer.flush();
        writer.close();
        os.close();
        conn.connect();

        int response = conn.getResponseCode();
        Log.d("HTTP", "The response is: " + response);
        is = conn.getInputStream();

        return is;
    }

    @NonNull
    private static String getQuery(List<AbstractMap.SimpleEntry<String, String>> params)
            throws UnsupportedEncodingException {
        StringBuilder result = new StringBuilder();
        boolean first = true;

        for (AbstractMap.SimpleEntry<String, String> pair : params) {
            if (first)
                first = false;
            else
                result.append("&");

            result.append(URLEncoder.encode(pair.getKey(), ENCODING));
            result.append("=");
            result.append(URLEncoder.encode(pair.getValue(), ENCODING));
        }

        return result.toString();
    }

}
