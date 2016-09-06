package com.naumchevski.iotcontrol.util;

import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.List;
import java.io.InputStream;

import com.google.gson.Gson;
import com.naumchevski.iotcontrol.model.CounterDTO;
import com.naumchevski.iotcontrol.model.CounterItem;

public final class CounterUtil {

    public static String webSocletURL(List<CounterItem> counterItems) {
        String baseURL = "ws://naumchevski.com/hub/open?";
        boolean first = true;
        for (CounterItem ci: counterItems) {
            if (!first) {
                baseURL += ";";
            }
            first = false;
            baseURL += ci.getControlId();
        }
        Log.i("CounterUtil", "base URL: " + baseURL);
        return baseURL;
    }
}