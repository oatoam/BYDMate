package com.toddmo.bydmate.client;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.toddmo.bydmate.client.utils.DataHolder;
import com.toddmo.bydmate.client.utils.KLog;
import com.toddmo.bydmate.server.BinderParcelable;

public class ServiceReadyReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        KLog.d("ServiceReadyReceiver onReceive: " + intent.getAction());
        if ("com.toddmo.bydmate.client.SERVICE_READY".equals(intent.getAction())) {
            BinderParcelable binderParcelable = intent.getParcelableExtra("service");
            DataHolder.put("service", binderParcelable);
        }
    }
}