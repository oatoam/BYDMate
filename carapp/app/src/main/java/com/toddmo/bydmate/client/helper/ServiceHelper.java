package com.toddmo.bydmate.client.helper;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.toddmo.bydmate.client.utils.KLog;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class ServiceHelper {

    public static final String TAG = "ServiceHelper";


    public static void startForegroundService(Context context, Intent intent) {
        try {
            Class<?> clz = Class.forName("android.content.Context");
            Method startForegroundService = clz.getDeclaredMethod("startForegroundService", Intent.class);
            startForegroundService.setAccessible(true);
            startForegroundService.invoke(context, intent);
            KLog.d("startForegroundService success");
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException |
                 InvocationTargetException e) {
            e.printStackTrace();
            KLog.d("startForegroundService failed, error: " + e);
        }
    }
}
