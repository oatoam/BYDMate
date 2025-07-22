package com.toddmo.bydmate.client.utils;

import android.location.Location;
import android.os.Build;

import org.json.JSONException;
import org.json.JSONObject;

public class LocationUtils {
    public static JSONObject locationToJson(Location location) {
        if (location == null) {
            return null;
        }
        JSONObject json = new JSONObject();
        try {
            json.put("latitude", location.getLatitude());
            json.put("longitude", location.getLongitude());
            if (location.hasAltitude()) json.put("altitude", location.getAltitude());
            if (location.hasAccuracy()) json.put("accuracy", location.getAccuracy());
            if (location.hasBearing()) json.put("bearing", location.getBearing());
            if (location.hasSpeed()) json.put("speed", location.getSpeed());
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (location.hasSpeedAccuracy()) {
                    json.put("speedAccuracy", location.getSpeedAccuracyMetersPerSecond());
                }
                if (location.hasBearingAccuracy()) {
                    json.put("BearingAccuracy", location.getBearingAccuracyDegrees());
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json;
    }

    public static String locationToString(Location location) {
        if (location == null) {
            return null;
        }
        JSONObject json = new JSONObject();
        try {
            json.put("latitude", location.getLatitude());
            json.put("longitude", location.getLongitude());
            if (location.hasAltitude()) json.put("altitude", location.getAltitude());
            if (location.hasAccuracy()) json.put("accuracy", location.getAccuracy());
            if (location.hasBearing()) json.put("bearing", location.getBearing());
            if (location.hasSpeed()) json.put("speed", location.getSpeed());
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (location.hasSpeedAccuracy()) {
                    json.put("speedAccuracy", location.getSpeedAccuracyMetersPerSecond());
                }
                if (location.hasBearingAccuracy()) {
                    json.put("BearingAccuracy", location.getBearingAccuracyDegrees());
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json.toString();
    }
}
