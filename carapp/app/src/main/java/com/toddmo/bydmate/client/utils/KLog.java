package com.toddmo.bydmate.client.utils;

import android.util.Log;

public class KLog {

    final static String TAG = "Klog";

    private static void print(int level, String content) {
        StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
        Log.println(level, TAG, stackTraceElements[4].getMethodName() + ":" + content);
    }

    public static void e(String content) {
        print(Log.ERROR, content);
    }

    public static void e() {
        e("");
    }

    public static void e(Exception ex) {
        e(ex.toString());
    }

    public static void e(Object obj) {
        e(obj.toString());
    }

    public static void w(String content) {
        print(Log.WARN, content);
    }

    public static void w() {
        w("");
    }

    public static void w(Exception ex) {
        w(ex.toString());
    }

    public static void w(Object obj) {
        w(obj.toString());
    }

    public static void i(String content) {
        print(Log.INFO, content);
    }

    public static void i() {
        i("");
    }

    public static void i(Exception ex) {
        i(ex.toString());
    }

    public static void i(Object obj) {
        i(obj.toString());
    }

    public static void d(String content) {
        print(Log.DEBUG, content);
    }

    public static void d() {
        d("");
    }

    public static void d(Exception ex) {
        d(ex.toString());
    }

    public static void d(Object obj) {
        d(obj.toString());
    }

    public static void v(String content) {
        print(Log.VERBOSE, content);
    }

    public static void v() {
        v("");
    }

    public static void v(Exception ex) {
        v(ex.toString());
    }

    public static void v(Object obj) {
        v(obj.toString());
    }
}
