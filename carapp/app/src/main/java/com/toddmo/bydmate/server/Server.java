package com.toddmo.bydmate.server;

import com.toddmo.bydmate.collector.DataListener;
import com.toddmo.bydmate.client.utils.KLog;

import android.os.Looper;

public class Server {
    public static void main(String[] args) {

        KLog.d("server running");

        DataListener listener = new DataListener(FakeContext.get());

        KLog.d("begin looping...");

        Looper.loop();
    }
}
