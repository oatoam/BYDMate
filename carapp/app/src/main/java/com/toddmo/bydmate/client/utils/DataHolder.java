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

    Map<String, Object> values = new HashMap<String, Object>();

    public static void put(String key, Object value) {
        getInstance().values.put(key, value);
    }

    public static void del(String key) {
        getInstance().values.remove(key);
    }

    public static String getString(String key) {
        return (String)getInstance().values.get(key);
    }
    public static Object getObject(String key) { return getInstance().values.get(key); }
}
