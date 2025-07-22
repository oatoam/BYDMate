package com.toddmo.bydmate.client.utils;

import java.util.HashMap;
import java.util.Map;

public class DataHolder {

    static DataHolder sInstance;
    public static DataHolder getInstance() {
        if (sInstance != null) {
            return sInstance;
        }

        sInstance = new DataHolder();
        return sInstance;
    }

    Map<String, String> values = new HashMap<String, String>();

    public static void put(String key, String value) {
        getInstance().values.put(key, value);
    }

    public static String get(String key) {
        return getInstance().values.get(key);
    }
}
