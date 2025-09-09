package com.toddmo.bydmate.client;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.toddmo.bydmate.client.helper.AdbHelper;
import com.toddmo.bydmate.client.helper.FileUtils;
import com.toddmo.bydmate.client.BYD.BydManifest;
import com.toddmo.bydmate.client.utils.DataHolder;
import com.toddmo.bydmate.client.utils.KLog;

public class DebuggingActivity extends AppCompatActivity {

    private final String TAG = DebuggingActivity.class.getName();

    private SharedPreferences preferences;
    Handler mainHandler = null;

    public static final String[] BYD_PERMISSIONS = {
            BydManifest.permission.BYDAUTO_BODYWORK_COMMON,
            BydManifest.permission.BYDAUTO_AC_COMMON,
            BydManifest.permission.BYDAUTO_PANORAMA_COMMON,
            BydManifest.permission.BYDAUTO_SETTING_COMMON,
            BydManifest.permission.BYDAUTO_INSTRUMENT_COMMON,
            BydManifest.permission.BYDAUTO_DOOR_LOCK_COMMON,
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_debugging);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.debug_main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // 初始化所有按钮点击监听器
        findViewById(R.id.set_fast_mode_btn).setOnClickListener(mClickListener);
        findViewById(R.id.set_boot_with_start_btn).setOnClickListener(mClickListener);
        findViewById(R.id.check_floating_permission_btn).setOnClickListener(mClickListener);
        findViewById(R.id.check_byd_permission_btn).setOnClickListener(mClickListener);
        findViewById(R.id.jump_to_main_btn).setOnClickListener(mClickListener);
        findViewById(R.id.stop_service_btn).setOnClickListener(mClickListener);
        findViewById(R.id.enable_autostart_btn).setOnClickListener(mClickListener);
        findViewById(R.id.disable_autostart_btn).setOnClickListener(mClickListener);

        mainHandler = new Handler(getMainLooper());

//        // 设置日志回调
//        KLog.setLogCallback(new KLog.LogCallback() {
//            @Override
//            public void onLog(String log) {
//                mainHandler.post(new Runnable() {
//                    @Override
//                    public void run() {
//                        TextView logview = findViewById(R.id.logview);
//                        if (logview == null) { return; }
//                        logview.setText(logview.getText() + "\n" + log);
//                    }
//                });
//            }
//        });

        KLog.startReceiveUDPLog();
        KLog.sendUDPLog("DebuggingActivity started: " + TAG);

        preferences = PreferenceManager.getDefaultSharedPreferences(this);

        boolean autostartEnabled = preferences.getBoolean("autostart_enabled", true);
        if (autostartEnabled) {
            startService();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
//        KLog.setLogCallback(null);
//        KLog.stopReceiveUDPLog();
    }

    private boolean isBydAutoPermissionGranted() {
        for (String perm : BYD_PERMISSIONS) {
            if (getBaseContext().checkSelfPermission(perm) != PackageManager.PERMISSION_GRANTED) {
                KLog.e(String.format("permission %s not granted", perm));
                return false;
            }
        }
        return true;
    }

    View.OnClickListener mClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            int id = v.getId();
            if (id == R.id.check_floating_permission_btn) {
                if (Settings.canDrawOverlays(DebuggingActivity.this)) {
                    Toast.makeText(DebuggingActivity.this, "浮窗权限已授予", Toast.LENGTH_SHORT).show();
                } else {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
                    intent.setData(Uri.parse("package:" + getPackageName()));
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                }
            } else if (id == R.id.check_byd_permission_btn) {
                boolean granted = isBydAutoPermissionGranted();
                if (granted) {
                    Toast.makeText(DebuggingActivity.this, "比亚迪车辆权限已授予", Toast.LENGTH_SHORT).show();
                } else {
                    ActivityCompat.requestPermissions(DebuggingActivity.this, BYD_PERMISSIONS, 125);
                }
            } else if (id == R.id.set_fast_mode_btn) {
                try {
                    Intent intent = new Intent();
                    intent.setComponent(new ComponentName("com.byd.rapidmode", "com.byd.rapidmode.RapidModeActivity"));
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(DebuggingActivity.this, "跳转极速模式失败，请手动打开", Toast.LENGTH_SHORT).show();
                }
            } else if (id == R.id.set_boot_with_start_btn) {
                try {
                    Intent intent = new Intent();
                    intent.setComponent(new ComponentName("com.byd.appstartmanagement", "com.byd.appstartmanagement.frame.AppStartManagement"));
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(DebuggingActivity.this, "跳转禁止自启动失败，请手动打开", Toast.LENGTH_SHORT).show();
                }
            } else if (id == R.id.jump_to_main_btn) {
                startService();
            } else if (id == R.id.stop_service_btn) {
                BackgroundMainService.stop(DebuggingActivity.this);
            } else if (id == R.id.enable_autostart_btn) {
                setBootCompleteReceiverEnabled(true);
                preferences.edit().putBoolean("autostart_enabled", true).apply();
                Toast.makeText(DebuggingActivity.this, "开机自启动已启用", Toast.LENGTH_SHORT).show();
            } else if (id == R.id.disable_autostart_btn) {
                setBootCompleteReceiverEnabled(false);
                preferences.edit().putBoolean("autostart_enabled", false).apply();
                Toast.makeText(DebuggingActivity.this, "开机自启动已禁用", Toast.LENGTH_SHORT).show();
            }
        }
    };

    void startService() {
//        if (!Settings.canDrawOverlays(DebuggingActivity.this)) {
//            Toast.makeText(DebuggingActivity.this, "请检查浮窗权限", Toast.LENGTH_SHORT).show();
//            return;
//        }
//        if (!isBydAutoPermissionGranted()) {
//            Toast.makeText(DebuggingActivity.this, "请检查车辆权限", Toast.LENGTH_SHORT).show();
//            return;
//        }

        BackgroundMainService.start(DebuggingActivity.this, "debuggingactivity");
    }

    private void setBootCompleteReceiverEnabled(boolean enabled) {
        // ComponentName componentName = new ComponentName(this, BootCompleteReceiver.class);
        // PackageManager packageManager = getPackageManager();
        // packageManager.setComponentEnabledSetting(
        //         componentName,
        //         enabled ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED : PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
        //         PackageManager.DONT_KILL_APP
        // );
    }
}