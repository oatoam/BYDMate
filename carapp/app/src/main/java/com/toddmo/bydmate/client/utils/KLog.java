package com.toddmo.bydmate.client.utils;

import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

public class KLog {

    final static String TAG = "Klog";

    public interface LogCallback {
        void onLog(String log);
    }

    private static LogCallback sCallback;

    public static final int LOG_UDP_PORT = 13579;

    public static void sendUDPLog(String message) {
        new Thread(() -> {
            try {
                InetAddress serverAddress = InetAddress.getByName("127.0.0.1");
                byte[] data = message.getBytes();
                DatagramPacket packet = new DatagramPacket(data, data.length, serverAddress, KLog.LOG_UDP_PORT);
                DatagramSocket socket = new DatagramSocket();
                socket.send(packet);
                socket.close();
            } catch (UnknownHostException e) {
                KLog.e("Unknown host: " + e);
            } catch (IOException e) {
                KLog.e("Error sending UDP log" + e);
            }
        }).start();
    }

    static boolean udprunning = false;
    static DatagramSocket logListenSocket;
    static Thread logListenThread = null;
    public static void startReceiveUDPLog() {
        udprunning = true;
        try {
            logListenSocket = new DatagramSocket(KLog.LOG_UDP_PORT);
            logListenThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    while (udprunning) {
                        try {
                            byte[] buffer = new byte[1024]; // 缓冲区大小
                            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                            logListenSocket.receive(packet);
                            String message = new String(packet.getData(), 0, packet.getLength());
                            KLog.d("R:" + message);
                        } catch (IOException e) {
                            KLog.e(e);
                        }
                    }

                }
            });
            logListenThread.start();
        } catch (SocketException e) {
            KLog.e(e);
        }
    }

    public static void stopReceiveUDPLog() {
        udprunning = false;
        try {
            logListenThread.join();
        } catch (InterruptedException e) {
            KLog.e(e);
        }
        if (logListenSocket != null) {
            logListenSocket.close();
            KLog.i("UDP socket closed");
        }
    }

    public static void setLogCallback(LogCallback callback) {
        sCallback = callback;
    }

    private static void print(int level, String content) {
        StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
        String log = stackTraceElements[4].getMethodName() + ":" + content;
        Log.println(level, TAG, log);
        if (sCallback != null) {
            sCallback.onLog(log);
        }
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
