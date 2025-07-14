package com.toddmo.bydmate.client;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import com.toddmo.bydmate.client.utils.KLog;



public class BootCompleteReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        KLog.d("BootCompleteReceiver onReceive: " + intent.getAction());
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
            boolean autostartEnabled = preferences.getBoolean("autostart_enabled", true); // 默认启用
            if (autostartEnabled) {
                BackgroundMainService.start(context, "boot_complete");
            }
        }
    }
}