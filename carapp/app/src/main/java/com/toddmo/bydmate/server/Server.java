package com.toddmo.bydmate.server;

import com.toddmo.bydmate.collector.DataListener;
import com.toddmo.bydmate.client.utils.KLog;

import android.os.Looper;

public class Server {

    final static String TAG = Server.class.getName();

    public static void main(String[] args) {



        KLog.setLogCallback(new KLog.LogCallback() {
            @Override
            public void onLog(String log) {
                KLog.sendUDPLog(log);
            }
        });

        KLog.sendUDPLog("test udp log from " + TAG);

        KLog.d("server running");

        BydMateServer.initialize();

//        DataListener listener = new DataListener(FakeContext.get());

        KLog.d("begin looping...");

        Looper.loop();
    }
}
