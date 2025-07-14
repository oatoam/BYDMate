package com.toddmo.bydmate.client.helper;

import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.util.Log;

import androidx.annotation.RequiresApi;

import com.tananaev.adblib.AdbBase64;
import com.tananaev.adblib.AdbConnection;
import com.tananaev.adblib.AdbCrypto;
import com.tananaev.adblib.AdbStream;
import com.toddmo.bydmate.client.helper.FileUtils;
import com.toddmo.bydmate.client.utils.KLog;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
import java.net.Socket;


public class AdbHelper {

    public static final String ADB_COMMAND = "adb";
    public static final String ADB_SHELL_COMMAND = "shell";
    public static final String ADB_INSTALL_COMMAND = "install";
    public static final String ADB_UNINSTALL_COMMAND = "uninstall";
    public static final String ADB_PUSH_COMMAND = "push";
    public static final String ADB_PULL_COMMAND = "pull";

    private static final String TAG = AdbHelper.class.getSimpleName();


    private static Object sLock = new Object();
    private static AdbHelper sInstance;

    public static AdbHelper getInstance() {
        if (sInstance == null) {
            synchronized (sLock) {
                if (sInstance == null) {
                    sInstance = new AdbHelper();
                }
            }
        }

        return sInstance;
    }

    AdbBase64 mAdbBase64 = new AdbBase64() {
        @RequiresApi(api = Build.VERSION_CODES.O)
        @Override
        public String encodeToString(byte[] data) {
            //                return DatatypeConverter.printBase64Binary(data);;
            return new String(Base64.getEncoder().encode(data));
        }
    };
    AdbCrypto mCrypto = null;
    public boolean setupCrypto(Context context) {
        String sdcardPath = FileUtils.getSDCardPath(context);
        File privateKeyFile = new File(sdcardPath + "/private.key");
        File publicKeyFile = new File(sdcardPath + "/public.key");
        try {
            if (privateKeyFile.exists() && publicKeyFile.exists()) {
                mCrypto = AdbCrypto.loadAdbKeyPair(mAdbBase64, privateKeyFile, publicKeyFile);
                KLog.d(String.format("load adb key from %s %s", privateKeyFile.getPath(), publicKeyFile.getPath()));
                return true;
            }
        } catch (IOException | NoSuchAlgorithmException | InvalidKeySpecException e) {
//            throw new RuntimeException(e);
        }

        try {
            mCrypto = AdbCrypto.generateAdbKeyPair(mAdbBase64);
            mCrypto.saveAdbKeyPair(privateKeyFile, publicKeyFile);
            KLog.d(String.format("saved adb key to %s %s", privateKeyFile.getPath(), publicKeyFile.getPath()));
            return true;
        } catch (NoSuchAlgorithmException e) {
//            throw new RuntimeException(e);
        } catch (IOException e) {
//            throw new RuntimeException(e);
        }

        return false;
    }

    public boolean checkConnectable(String protocol, String address, int port) {
        try {
            if (protocol.equalsIgnoreCase("tcp")) {
                Socket socket = new Socket(address, port);
                socket.close();
                return true;
            } else if (protocol.equalsIgnoreCase("udp")) {
                java.net.DatagramSocket socket = new java.net.DatagramSocket();
                socket.connect(java.net.InetAddress.getByName(address), port);
                socket.close();
                return true;
            }
        } catch (IOException e) {
        }
        return false;
    }

    
    public class ADBCommandThread extends Thread {

        public String mRemoteHost;
        public int mRemotePort;
        public String mShellCommand;
        boolean mInterrupted;

        public ADBCommandThread(String remoteHost, int remotePort, String shellCommand) {
            super();
            this.mRemoteHost = remoteHost;
            this.mRemotePort = remotePort;
            this.mShellCommand = shellCommand;
            mInterrupted = false;
        }
        @Override
        public void run() {
            AdbConnection connection = null;
            try {

                Socket socket = new Socket(mRemoteHost, mRemotePort);

                AdbCrypto crypto = mCrypto;

                if (crypto == null) {
                    KLog.e("no valid adb crypto, create a temp one");
                    crypto = AdbCrypto.generateAdbKeyPair(mAdbBase64);

                    return;
                }

                connection = AdbConnection.create(socket, crypto);

                connection.connect();

                AdbStream stream = connection.open("shell:" + mShellCommand);
//                    AdbStream stream = connection.open("shell:/data/local/tmp/run.sh hello world");

                try {
                    while (!mInterrupted) {
                        byte[] data = stream.read();
                        Log.v(TAG, "shell output: " + new String(data));
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            } finally {
                try {
                    if (connection != null) {
                        connection.close();
                        connection = null;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        }

        public void interrupt() {
            mInterrupted = true;
        }
    }

    public ADBCommandThread sendAdbShellCommand(String shellCommand) {
        String remoteHost = "127.0.0.1";
        int remotePort = 5555;

        if (!checkConnectable("tcp", remoteHost, remotePort)) {
            KLog.e(String.format("%s:%d is not connectable", remoteHost, remotePort));
            return null;
        }

        ADBCommandThread thread = new ADBCommandThread(remoteHost, remotePort, shellCommand);
        thread.start();

        return thread;
    }

    public static void testAdbConnection(Context context) {

        AdbHelper adb = AdbHelper.getInstance();

        if (!adb.setupCrypto(context)) {
            KLog.e("failed to setup crypto");
            return;
        }

        final Handler mainHandler = new Handler();
        new Thread(new Runnable() {
            @Override
            public void run() {
                AdbHelper.ADBCommandThread command =
                        adb.sendAdbShellCommand(String.format("ping 127.0.0.1"));

                if (command == null) {
                    com.toddmo.bydmate.client.utils.KLog.e("sendAdbShellCommand failed");
                    return;
                }

                mainHandler.postDelayed(() -> {
                    command.interrupt();
                }, 5000);
            }
        }).start();
    }
}