package com.toddmo.bydmate.server;

import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.view.InputEvent;
import android.view.MotionEvent;

import androidx.annotation.NonNull;

import com.toddmo.bydmate.aidl.IBydMateServer;

public class BydMateServer {

    InputManager inputManager = InputManager.create();
    public class Service extends IBydMateServer.Stub {

        @Override
        public void injectInputEvent(InputEvent event, int displayId) throws RemoteException {
            if (displayId >= 0) {
                InputManager.setDisplayId(event, displayId);
            }
            if (event instanceof MotionEvent && ((MotionEvent) event).getAction() == 0) {
                InputManager.syncInputTransactions();
            }

            inputManager.injectInputEvent(event, InputManager.INJECT_INPUT_EVENT_MODE_ASYNC);

            if (event instanceof MotionEvent && (((MotionEvent) event).getAction() == 1
                    || ((MotionEvent) event).getAction() == 3)) {
                InputManager.syncInputTransactions();
            }
        }


    }

    private Service mService = new Service();
    private static BydMateServer sInstance = null;


    public static void initialize() {
        sInstance = new BydMateServer();
        Intent intent = new Intent();
        intent.setPackage("com.toddmo.bydmate.client");
        intent.setAction("com.toddmo.bydmate.client.SERVICE_READY");
        intent.putExtra("service", new BinderParcelable(sInstance.mService));
        FakeContext.get().sendBroadcast(intent);
    }
}
