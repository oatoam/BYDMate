package com.toddmo.bydmate.server;

import android.os.Parcelable;
import android.os.ResultReceiver;

import com.toddmo.bydmate.client.utils.KLog;

public class CarControl {

    private static CarControl sInstance = null;
    public static CarControl getInstance() {
        if (sInstance == null) {
            sInstance = new CarControl();
        }
        return sInstance;
    }


}
