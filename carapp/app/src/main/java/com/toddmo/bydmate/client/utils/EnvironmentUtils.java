package com.toddmo.bydmate.client.utils;

public class EnvironmentUtils {
    public static boolean isEmulator() {
        boolean isEmulator = android.os.Build.FINGERPRINT.contains("generic")
                || android.os.Build.MODEL.contains("google_sdk")
                || android.os.Build.MODEL.contains("Emulator")
                || android.os.Build.MODEL.contains("Android SDK built for x86")
                || android.os.Build.MANUFACTURER.contains("Genymotion")
                || (android.os.Build.BRAND.startsWith("generic") && android.os.Build.DEVICE.startsWith("generic"))
                || "google_sdk".equals(android.os.Build.PRODUCT);
        return isEmulator;
    }
}
