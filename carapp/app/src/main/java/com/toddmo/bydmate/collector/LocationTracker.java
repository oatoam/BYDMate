package com.toddmo.bydmate.collector;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import androidx.core.content.ContextCompat;
import com.toddmo.bydmate.client.utils.KLog;

public class LocationTracker {

    private static final String TAG = "LocationTracker";
    private Context context;
    private DataProcesser dataProcesser;
    private LocationManager locationManager;
    private LocationListener locationListener;

    public LocationTracker(Context context, DataProcesser dataProcesser) {
        this.context = context;
        this.dataProcesser = dataProcesser;
        this.locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        initLocationListener();
    }

    private void initLocationListener() {
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                if (location != null) {
                    String locationData = String.format("Latitude: %f, Longitude: %f, Altitude: %f, Speed: %f, Bearing: %f, Accuracy: %f, Provider: %s",
                            location.getLatitude(),
                            location.getLongitude(),
                            location.getAltitude(),
                            location.getSpeed(),
                            location.getBearing(),
                            location.getAccuracy(),
                            location.getProvider());
                    KLog.d(TAG, "Location Updated: " + locationData);
                    dataProcesser.put("location", location.getProvider(), locationData);
                }
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
                KLog.d(TAG, "Provider Status Changed: " + provider + ", Status: " + status);
            }

            @Override
            public void onProviderEnabled(String provider) {
                KLog.d(TAG, "Provider Enabled: " + provider);
            }

            @Override
            public void onProviderDisabled(String provider) {
                KLog.d(TAG, "Provider Disabled: " + provider);
            }
        };
    }

    public void startTracking() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            KLog.e(TAG, "Location permissions not granted.");
            // In a real app, you would request permissions here.
            return;
        }

        try {
            // Request location updates from GPS provider
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 1, locationListener);
                KLog.d(TAG, "Started tracking with GPS_PROVIDER.");
            } else {
                KLog.w(TAG, "GPS_PROVIDER is not enabled.");
            }

            // Request location updates from Network provider (for Beidou or other network-based location)
            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000, 1, locationListener);
                KLog.d(TAG, "Started tracking with NETWORK_PROVIDER.");
            } else {
                KLog.w(TAG, "NETWORK_PROVIDER is not enabled.");
            }
        } catch (SecurityException e) {
            KLog.e(TAG, "SecurityException: " + e.getMessage());
        }
    }

    public void stopTracking() {
        if (locationManager != null && locationListener != null) {
            locationManager.removeUpdates(locationListener);
            KLog.d(TAG, "Stopped location tracking.");
        }
    }
}