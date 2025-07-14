package com.toddmo.bydmate.client;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import com.toddmo.bydmate.client.helper.AdbHelper;
import com.toddmo.bydmate.client.helper.FileUtils;
import com.toddmo.bydmate.client.utils.KLog;

public class BackgroundMainService extends Service {

    private static String TAG = BackgroundMainService.class.getName();

    final String SH_NAME = "bydmate.run.sh";
    final String CLASS_NAME = "com.toddmo.bydmate.server.Server";

    private static final String CHANNEL_ID = "ForegroundServiceChannel";

    public static void start(Context context, String reason) {
        Intent intent = new Intent(context, BackgroundMainService.class);
        intent.putExtra("reason", reason);
        if (Build.VERSION.SDK_INT >= 26) {
            ServiceHelper.startForegroundService(context, intent);
        } else {
            context.startService(intent);
        }
    }

    public static void stop(Context context) {
        Intent intent = new Intent(context, BackgroundMainService.class);
        context.stopService(intent);
    }

    public BackgroundMainService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();

        KLog.d("BackgroundMainService onCreate");
    }

   @Override
    public int onStartCommand(Intent intent, int flags, int startId) {


        Log.d(TAG, "BackgroundMainService is running");

        String reason = intent.getStringExtra("reason");

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        // getSharedPreferences("preferences", MODE_PRIVATE);

        // 创建通知
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("BYDMate前台服务")
                .setContentText(reason)
                .build();

        startForeground(1, notification);

        // 在这里运行原来的逻辑
        // runOriginalLogic();

       //                AdbHelper.testAdbConnection(SplashActivity.this);

       new Thread(new Runnable() {
           @Override
           public void run() {
               AdbHelper adb = AdbHelper.getInstance();
               Context ctx = getBaseContext();
               adb.setupCrypto(ctx);
               String sdcard = FileUtils.getSDCardPath(ctx);

               String sourceDir = getApplicationContext().getApplicationInfo().sourceDir;



               String shPath = sdcard + "/" + SH_NAME;

               FileUtils.copyAssetFileToDir(ctx, SH_NAME, sdcard);

               adb.sendAdbShellCommand(String.format("mkdir -p /data/local/tmp/bydmate/"));
               try { Thread.sleep(300); } catch (Exception e) {}
               adb.sendAdbShellCommand(String.format("cp -v %s /data/local/tmp/bydmate/", shPath));
               try { Thread.sleep(300); } catch (Exception e) {}

               adb.sendAdbShellCommand(String.format("chmod +x /data/local/tmp/bydmate/%s", SH_NAME));
               try { Thread.sleep(300); } catch (Exception e) {}

               adb.sendAdbShellCommand(String.format("pkill -f %s", CLASS_NAME));
               try { Thread.sleep(300); } catch (Exception e) {}

               adb.sendAdbShellCommand(String.format("/data/local/tmp/bydmate/%s " +
                       "%s %s", SH_NAME, sourceDir, CLASS_NAME));
           }
       }).start();


       KLog.d("BackgroundMainService onStartCommand reason = " + reason);

       Toast.makeText(this, "bydmate后台服务已启动: " + reason, Toast.LENGTH_SHORT).show();

       return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void createNotificationChannel() {
        NotificationChannel serviceChannel = new NotificationChannel(
                CHANNEL_ID,
                "前台服务通知",
                NotificationManager.IMPORTANCE_DEFAULT
        );

        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.createNotificationChannel(serviceChannel);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        new Thread(new Runnable() {
            @Override
            public void run() {
                AdbHelper adb = AdbHelper.getInstance();
                adb.sendAdbShellCommand(String.format("pkill -f %s", CLASS_NAME));
            }
        }).start();


        KLog.d("onDestory");
        Toast.makeText(this, "bydmate后台服务已停止", Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        KLog.d("onUnbind");
        return super.onUnbind(intent);
    }
}