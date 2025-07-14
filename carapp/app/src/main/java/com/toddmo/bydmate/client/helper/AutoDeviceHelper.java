package com.toddmo.bydmate.client.helper;

import android.hardware.bydauto.AbsBYDAutoDevice;
import android.util.Log;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class AutoDeviceHelper {
    public static Object get(AbsBYDAutoDevice bydAutoDevice, int deviceType, int featuresId) {
        try {
            Class<?> clz = Class.forName("android.hardware.bydauto.AbsBYDAutoDevice");
            Method get = clz.getDeclaredMethod("get", int.class, int.class);
            get.setAccessible(true);
            Object result = get.invoke(bydAutoDevice, deviceType, featuresId);
            Log.i("AutoDeviceHelper", "获取features对应数据：" + featuresId + " --> " + result);
            return result;
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException |
                 InvocationTargetException e) {
            e.printStackTrace();
        }
        return 0;
    }
}