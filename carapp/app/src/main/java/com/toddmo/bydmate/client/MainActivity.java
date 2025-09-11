package com.toddmo.bydmate.client;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.hardware.display.VirtualDisplay;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.toddmo.bydmate.aidl.IBydMateServer;
import com.toddmo.bydmate.client.utils.DataHolder;
import com.toddmo.bydmate.client.utils.KLog;
import com.toddmo.bydmate.client.widgets.ApplicationPIPView;
import com.toddmo.bydmate.client.widgets.FloatingWindow;
import com.toddmo.bydmate.client.widgets.InfoBar;
import com.toddmo.bydmate.server.BinderParcelable;
import com.toddmo.bydmate.server.KeyboardMonitorTest;

import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private final String TAG = MainActivity.class.getName();

    Handler mainHandler = null;

    // UI组件
    private ConstraintLayout mainContainer;
    private View dividerVertical;
    private View dividerHorizontal;
    private ApplicationPIPView primaryRegion;
    private ApplicationPIPView secondaryRegion;
    private InfoBar infoBar;
    private FloatingWindow floatingWindow;

    // 比例控制
    private float primaryRatio = 0.5f; // 主界面占比，范围0.1-0.9
    private boolean isLandscape = true;

    // Surface模式下不需要ADB相关变量

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 只隐藏状态栏，保留导航栏
        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_FULLSCREEN |
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        );

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main_new);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_container), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Surface模式下不需要ADB初始化

        initializeViews();
        setupDividerTouchListener();
        updateLayoutForOrientation();

        mainHandler = new Handler(getMainLooper());

//        KLog.setLogCallback(new KLog.LogCallback() {
//            @Override
//            public void onLog(String log) {
//                mainHandler.post(new Runnable() {
//                    @Override
//                    public void run() {
//                        // 可以在这里处理日志，但MainActivity现在主要用于画中画显示
//                        KLog.d("MainActivity log: " + log);
//                    }
//                });
//            }
//        });

        // 检查权限
        checkPermissions();
    }

    private void initializeViews() {
        mainContainer = findViewById(R.id.main_container);
        dividerVertical = findViewById(R.id.divider_vertical);
        dividerHorizontal = findViewById(R.id.divider_horizontal);
        primaryRegion = findViewById(R.id.primary_region);
        secondaryRegion = findViewById(R.id.secondary_pip_view);
        infoBar = findViewById(R.id.info_bar);

        // 初始化悬浮窗
        floatingWindow = new FloatingWindow(this);

        // 设置ApplicationPIPView的监听器
        setupPIPViewListeners();

        // 设置信息栏显示模式
        updateInfoBarDisplayMode();

//         显示悬浮窗
        floatingWindow.show();

        // 初始化ADB连接（用于ADB模式）
        com.toddmo.bydmate.client.helper.AdbHelper adbHelper =
            com.toddmo.bydmate.client.helper.AdbHelper.getInstance();
        adbHelper.setupCrypto(this);
    }

    private void injectKeyEvent(int keycode) {
        KLog.d("injectKeyEvent : " + keycode);
        new Thread(new Runnable() {
            @Override
            public void run() {
                BinderParcelable binder = (BinderParcelable) DataHolder.getObject("service");
                if (binder == null) {
                    KLog.e("onBackClick: no valid service");
                    return;
                }
                IBydMateServer server = binder.server;
                try {
                    long downtime = SystemClock.uptimeMillis();
                    KeyEvent downEvent = new KeyEvent(downtime, downtime, KeyEvent.ACTION_DOWN, keycode, 0);
                    server.injectInputEvent(downEvent, -1);
                    Thread.sleep(1);
                    long uptime = SystemClock.uptimeMillis();
                    KeyEvent upEvent = new KeyEvent(uptime, uptime, KeyEvent.ACTION_UP, keycode, 0);
                    server.injectInputEvent(upEvent, -1);
                } catch (RemoteException e) {
                    KLog.e("onBackClick: failed to injectInputEvent ");
                } catch (InterruptedException e) {
                    KLog.e("onBackClick: failed to injectInputEvent sleep");
                }
            }
        }).start();

    }

    private void setupPIPViewListeners() {
        // ADB模式下不需要权限请求监听器
        // 主界面和副界面都使用ADB直接创建VirtualDisplay

        // 应用选择监听器
        primaryRegion.setOnAppSelectedListener(packageName -> {
            KLog.d(TAG + " Primary region selected app: " + packageName);
        });

        secondaryRegion.setOnAppSelectedListener(packageName -> {
            KLog.d(TAG + " Secondary region selected app: " + packageName);
        });

        // 设置悬浮窗按钮监听器
        if (floatingWindow != null) {
            floatingWindow.setOnButtonClickListener(new FloatingWindow.OnButtonClickListener() {
                @Override
                public void onBackClick() {
                    // 处理返回按钮点击
//                    performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK);
                    injectKeyEvent(KeyEvent.KEYCODE_BACK);
                }

                @Override
                public void onHomeClick() {
                    // 处理首页按钮点击
//                    performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_HOME);
                    injectKeyEvent(KeyEvent.KEYCODE_HOME);
                }

                @Override
                public void onRecentClick() {
                    // 处理多任务按钮点击
//                    performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_RECENTS);
                    injectKeyEvent(KeyEvent.KEYCODE_APP_SWITCH);
                }
            });
        }
    }

    private void performGlobalAction(int action) {
        // 这里需要实现全局操作，暂时使用Intent方式
        try {
            switch (action) {
                case android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK:
                    // 返回操作
                    break;
                case android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_HOME:
                    // 首页操作
                    Intent homeIntent = new Intent(Intent.ACTION_MAIN);
                    homeIntent.addCategory(Intent.CATEGORY_HOME);
                    homeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(homeIntent);
                    break;
                case android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_RECENTS:
                    // 多任务操作
                    break;
            }
        } catch (Exception e) {
            KLog.e(TAG + " Failed to perform global action: " + e.getMessage());
        }
    }

    private void setupDividerTouchListener() {
        // 设置调节杆的触摸监听器
        dividerVertical.setOnTouchListener(new DividerTouchListener(this::updatePrimaryRatio, false)); // vertical
        dividerHorizontal.setOnTouchListener(new DividerTouchListener(this::updatePrimaryRatio, true)); // horizontal
    }

    private void updatePrimaryRatio(float ratio) {
        this.primaryRatio = Math.max(0.1f, Math.min(0.9f, ratio));
        updateLayoutConstraints();
    }

    private void swapRegions() {
        KLog.d(TAG + " Swapping primary and secondary regions");

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);

        boolean swap = preferences.getBoolean("swapregion", false);

        preferences.edit().putBoolean("swapregion", !swap).apply();

        boolean newswap = preferences.getBoolean("swapregion", false);

        KLog.d(String.format("swapregion from %s to %s", swap, newswap));
        // 更新布局约束
        updateLayoutConstraints();
    }

    private void updateLayoutConstraints() {
        ConstraintSet constraintSet = new ConstraintSet();
        constraintSet.clone(mainContainer);
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);

        boolean swap = preferences.getBoolean("swapregion", false);

        KLog.d("Updating layout constraints primaryRatio: " + primaryRatio + " swapregion: " + swap);
        // 记录容器尺寸
        KLog.d("Main container size: " + mainContainer.getWidth() + "x" + mainContainer.getHeight());

        if (isLandscape) {
            // 横屏模式：左右分布
            KLog.d("Setting landscape constraints");

            // 创建水平链约束
            constraintSet.createHorizontalChain(
                ConstraintSet.PARENT_ID, ConstraintSet.LEFT,
                ConstraintSet.PARENT_ID, ConstraintSet.RIGHT,
                new int[]{R.id.primary_region, R.id.secondary_region},
                null,
                ConstraintSet.CHAIN_SPREAD_INSIDE
            );

            // 设置权重
            constraintSet.setHorizontalWeight(R.id.primary_region, primaryRatio);
            constraintSet.setHorizontalWeight(R.id.secondary_region, 1.0f - primaryRatio);

            // 确保垂直方向占满高度
            constraintSet.connect(R.id.primary_region, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP);
            constraintSet.connect(R.id.primary_region, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM);
            constraintSet.connect(R.id.secondary_region, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP);
            constraintSet.connect(R.id.secondary_region, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM);

            if (swap) {
                constraintSet.connect(R.id.primary_region, ConstraintSet.START, R.id.divider_vertical, ConstraintSet.END);
                constraintSet.connect(R.id.primary_region, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END);
                constraintSet.connect(R.id.secondary_region, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START);
                constraintSet.connect(R.id.secondary_region, ConstraintSet.END, R.id.divider_vertical, ConstraintSet.START);

                // 确保divider位置正确
                constraintSet.connect(R.id.divider_vertical, ConstraintSet.START, R.id.secondary_region, ConstraintSet.END);
                constraintSet.connect(R.id.divider_vertical, ConstraintSet.END, R.id.primary_region, ConstraintSet.START);
                constraintSet.connect(R.id.divider_vertical, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP);
                constraintSet.connect(R.id.divider_vertical, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM);
            } else {
                constraintSet.connect(R.id.primary_region, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START);
                constraintSet.connect(R.id.primary_region, ConstraintSet.END, R.id.divider_vertical, ConstraintSet.START);
                constraintSet.connect(R.id.secondary_region, ConstraintSet.START, R.id.divider_vertical, ConstraintSet.END);
                constraintSet.connect(R.id.secondary_region, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END);

                // 确保divider位置正确
                constraintSet.connect(R.id.divider_vertical, ConstraintSet.START, R.id.primary_region, ConstraintSet.END);
                constraintSet.connect(R.id.divider_vertical, ConstraintSet.END, R.id.secondary_region, ConstraintSet.START);
                constraintSet.connect(R.id.divider_vertical, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP);
                constraintSet.connect(R.id.divider_vertical, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM);
            }


        } else {
            // 竖屏模式：上下分布
            KLog.d("Setting portrait constraints");

            // 创建垂直链约束
            constraintSet.createVerticalChain(
                ConstraintSet.PARENT_ID, ConstraintSet.TOP,
                ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM,
                new int[]{R.id.primary_region, R.id.secondary_region},
                null,
                ConstraintSet.CHAIN_SPREAD_INSIDE
            );

            // 设置权重
            constraintSet.setVerticalWeight(R.id.primary_region, primaryRatio);
            constraintSet.setVerticalWeight(R.id.secondary_region, 1.0f - primaryRatio);

            // 确保水平方向占满宽度
            constraintSet.connect(R.id.primary_region, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START);
            constraintSet.connect(R.id.primary_region, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END);
            constraintSet.connect(R.id.secondary_region, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START);
            constraintSet.connect(R.id.secondary_region, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END);

            if (swap) {
                constraintSet.connect(R.id.primary_region, ConstraintSet.TOP, R.id.divider_vertical, ConstraintSet.BOTTOM);
                constraintSet.connect(R.id.primary_region, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM);
                constraintSet.connect(R.id.secondary_region, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP);
                constraintSet.connect(R.id.secondary_region, ConstraintSet.BOTTOM, R.id.divider_vertical, ConstraintSet.TOP);
            } else {
                constraintSet.connect(R.id.primary_region, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP);
                constraintSet.connect(R.id.primary_region, ConstraintSet.BOTTOM, R.id.divider_vertical, ConstraintSet.TOP);
                constraintSet.connect(R.id.secondary_region, ConstraintSet.TOP, R.id.divider_vertical, ConstraintSet.BOTTOM);
                constraintSet.connect(R.id.secondary_region, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM);
            }

            // 确保divider位置正确
            constraintSet.connect(R.id.divider_horizontal, ConstraintSet.TOP, R.id.primary_region, ConstraintSet.BOTTOM);
            constraintSet.connect(R.id.divider_horizontal, ConstraintSet.BOTTOM, R.id.secondary_region, ConstraintSet.TOP);
            constraintSet.connect(R.id.divider_horizontal, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START);
            constraintSet.connect(R.id.divider_horizontal, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END);
        }



        constraintSet.applyTo(mainContainer);

        // 强制重新布局
        mainContainer.requestLayout();
        mainContainer.invalidate();

        KLog.d("Layout constraints applied, requesting layout refresh");

        // 添加延迟检查布局尺寸
        mainContainer.post(() -> {
            KLog.d("Post-layout check - Main container: " + mainContainer.getWidth() + "x" + mainContainer.getHeight());
            if (primaryRegion != null) {
                KLog.d("Primary region size: " + primaryRegion.getWidth() + "x" + primaryRegion.getHeight());
            }
            if (secondaryRegion != null) {
                KLog.d("Secondary region size: " + secondaryRegion.getWidth() + "x" + secondaryRegion.getHeight());
            }
        });
    }

    private void updateLayoutForOrientation() {
        KLog.d("Updating layout for orientation:");
        int orientation = getResources().getConfiguration().orientation;
        isLandscape = (orientation == Configuration.ORIENTATION_LANDSCAPE);

        if (isLandscape) {
            // 横屏：显示垂直调节杆，隐藏水平调节杆
            dividerVertical.setVisibility(View.VISIBLE);
            dividerHorizontal.setVisibility(View.GONE);
        } else {
            // 竖屏：显示水平调节杆，隐藏垂直调节杆
            dividerVertical.setVisibility(View.GONE);
            dividerHorizontal.setVisibility(View.VISIBLE);
        }

        updateLayoutConstraints();
        updateInfoBarDisplayMode();
    }

    private void updateInfoBarDisplayMode() {
        if (infoBar != null) {
            infoBar.setDisplayMode(isLandscape);
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        // 保存当前比例设置
        float currentRatio = primaryRatio;

        // 更新布局
        updateLayoutForOrientation();

        // 恢复比例设置
        primaryRatio = currentRatio;
        updateLayoutConstraints();

        // 通知ApplicationPIPView屏幕方向改变
        if (primaryRegion != null) {
            primaryRegion.onOrientationChanged(isLandscape);
        }
        if (secondaryRegion != null) {
            secondaryRegion.onOrientationChanged(isLandscape);
        }

        KLog.d(TAG + " Screen orientation changed to: " +
               (isLandscape ? "landscape" : "portrait"));
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            // 只隐藏状态栏，保留导航栏
            getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN |
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            );
        }
    }

    private void checkPermissions() {
        // 检查必要的权限
        if (!Settings.canDrawOverlays(this)) {
            requestOverlayPermission();
        }
    }

    private void requestOverlayPermission() {
        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
        intent.setData(Uri.parse("package:" + getPackageName()));
        startActivity(intent);
    }

    // ADB模式下不需要MediaProjection权限请求

    // ADB模式下不需要权限设置对话框

    @Override
    protected void onDestroy() {
        super.onDestroy();
        KLog.setLogCallback(null);

        // 销毁悬浮窗
        if (floatingWindow != null) {
            floatingWindow.destroy();
            floatingWindow = null;
        }
    }

    // 回调接口用于更新比例
    interface RatioUpdateCallback {
        void onRatioUpdate(float ratio);
    }

    // DividerTouchListener 内部类用于处理调节杆拖拽
    private class DividerTouchListener implements View.OnTouchListener {
        private final float minSlideDistance; // 最小滑动距离阈值（像素）
        private float lastX, lastY;
        private float initialX, initialY; // 初始触摸位置
        private float accumulatedTranslationX = 0; // 累积的水平位移
        private float accumulatedTranslationY = 0; // 累积的垂直位移
        private final RatioUpdateCallback callback;
        private final boolean isHorizontal;
        private final Handler handler = new Handler(getMainLooper());
        private boolean isInteractive = false; // 是否已进入交互状态
        private long lastClickTime = 0; // 上次点击时间
        private static final long DOUBLE_CLICK_TIME_DELTA = 300; // 双击时间间隔（毫秒）
        private final Runnable resetToDefaultRunnable = new Runnable() {
            @Override
            public void run() {
                resetToDefaultState();
            }
        };

        DividerTouchListener(RatioUpdateCallback callback, boolean isHorizontal) {
            this.callback = callback;
            this.isHorizontal = isHorizontal;

            // 根据调节杆尺寸计算最小滑动距离阈值
            if (isHorizontal) {
                // 水平调节杆：基于高度的1.5倍
                float dividerHeight = dividerHorizontal.getHeight();
                this.minSlideDistance = dividerHeight > 0 ? dividerHeight * 1.5f : 24.0f * 1.5f; // 默认24dp的1.5倍
            } else {
                // 垂直调节杆：基于宽度的1.5倍
                float dividerWidth = dividerVertical.getWidth();
                this.minSlideDistance = dividerWidth > 0 ? dividerWidth * 1.5f : 12.0f * 1.5f; // 默认12dp的1.5倍
            }
        }

        @Override
        public boolean onTouch(View v, android.view.MotionEvent event) {

            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
            boolean swapregion = preferences.getBoolean("swapregion", false);


            switch (event.getAction()) {
                case android.view.MotionEvent.ACTION_DOWN:
                    long clickTime = System.currentTimeMillis();
                    // 检测双击
                    if (clickTime - lastClickTime < DOUBLE_CLICK_TIME_DELTA) {
                        // 双击检测成功，交换区域位置
                        swapRegions();
                        lastClickTime = 0; // 重置点击时间
                        return true;
                    }
                    lastClickTime = clickTime;

                    initialX = event.getRawX();
                    initialY = event.getRawY();
                    lastX = initialX;
                    lastY = initialY;
                    // 重置累积位移
                    accumulatedTranslationX = 0;
                    accumulatedTranslationY = 0;
                    // 重置交互状态
                    isInteractive = false;
                    // 取消之前的延迟重置
                    handler.removeCallbacks(resetToDefaultRunnable);
                    // 不立即切换到交互状态，等待滑动距离超过阈值
                    return true;

                case android.view.MotionEvent.ACTION_MOVE:
                    float deltaX = event.getRawX() - lastX;
                    float deltaY = event.getRawY() - lastY;

                    // 计算从初始位置的总移动距离
                    float totalDeltaX = event.getRawX() - initialX;
                    float totalDeltaY = event.getRawY() - initialY;
                    float distance = (float) Math.sqrt(totalDeltaX * totalDeltaX + totalDeltaY * totalDeltaY);

                    // 如果移动距离未超过阈值，不进行任何操作
                    if (distance < minSlideDistance) {
                        return true;
                    }

                    // 超过阈值，进入交互状态
                    if (!isInteractive) {
                        isInteractive = true;
                        setInteractiveState(v);
                    }

                    if (isLandscape) {
                        // 横屏模式：水平移动divider
                        float containerWidth = mainContainer.getWidth();
                        if (containerWidth > 0) {
                            // 累积位移
                            accumulatedTranslationX += deltaX;
                            // 计算基于当前比例的divider中心位置
                            float currentDividerCenterX = (swapregion ? 1-primaryRatio : primaryRatio) * containerWidth;
                            // 计算新的divider中心位置
                            float newDividerCenterX = currentDividerCenterX + accumulatedTranslationX;
                            // 限制在合理范围内
                            float minCenterX = 0.1f * containerWidth;
                            float maxCenterX = 0.9f * containerWidth;
                            newDividerCenterX = Math.max(minCenterX, Math.min(maxCenterX, newDividerCenterX));
                            // 计算translation（相对于约束位置的偏移）
                            float translationX = newDividerCenterX - currentDividerCenterX;
                            // 更新divider位置（视觉反馈）
                            v.setTranslationX(translationX);
                        }
                    } else {
                        // 竖屏模式：垂直移动divider
                        float containerHeight = mainContainer.getHeight();
                        if (containerHeight > 0) {
                            // 累积位移
                            accumulatedTranslationY += deltaY;
                            // 计算基于当前比例的divider中心位置
                            float currentDividerCenterY = (swapregion ? 1-primaryRatio : primaryRatio) * containerHeight;
                            // 计算新的divider中心位置
                            float newDividerCenterY = currentDividerCenterY + accumulatedTranslationY;
                            // 限制在合理范围内
                            float minCenterY = 0.1f * containerHeight;
                            float maxCenterY = 0.9f * containerHeight;
                            newDividerCenterY = Math.max(minCenterY, Math.min(maxCenterY, newDividerCenterY));
                            // 计算translation（相对于约束位置的偏移）
                            float translationY = newDividerCenterY - currentDividerCenterY;
                            // 更新divider位置（视觉反馈）
                            v.setTranslationY(translationY);
                        }
                    }

                    lastX = event.getRawX();
                    lastY = event.getRawY();
                    return true;

                case android.view.MotionEvent.ACTION_UP:
                    // 只有在进入交互状态的情况下才进行布局调整
                    if (isInteractive) {
                        // 根据divider的最终位置计算比例
                        float finalRatio = (swapregion ? 1-primaryRatio : primaryRatio);
                        if (isLandscape) {
                            // 横屏模式：根据divider的最终位置计算比例
                            float containerWidth = mainContainer.getWidth();
                            if (containerWidth > 0) {
                                float currentDividerCenterX = (swapregion ? 1-primaryRatio : primaryRatio) * containerWidth;
                                float finalDividerCenterX = currentDividerCenterX + v.getTranslationX();
                                finalRatio = finalDividerCenterX / containerWidth;
                                finalRatio = Math.max(0.1f, Math.min(0.9f, finalRatio));
                            }
                        } else {
                            // 竖屏模式：根据divider的最终位置计算比例
                            float containerHeight = mainContainer.getHeight();
                            if (containerHeight > 0) {
                                float currentDividerCenterY = (swapregion ? 1-primaryRatio : primaryRatio) * containerHeight;
                                float finalDividerCenterY = currentDividerCenterY + v.getTranslationY();
                                finalRatio = finalDividerCenterY / containerHeight;
                                finalRatio = Math.max(0.1f, Math.min(0.9f, finalRatio));
                            }
                        }

                        // 重置divider的translation
                        v.setTranslationX(0);
                        v.setTranslationY(0);

                        // 应用最终比例并刷新布局
                        if (finalRatio != (swapregion ? 1-primaryRatio : primaryRatio)) {

                            if (swapregion) {
                                primaryRatio = 1 - finalRatio;
                            } else {
                                primaryRatio = finalRatio;
                            }
                            updateLayoutConstraints();
                            KLog.d(TAG + " Layout updated with final ratio: " + primaryRatio);
                        }

                        // 延迟切换回默认状态
                        handler.postDelayed(resetToDefaultRunnable, 3000); // 500ms延迟
                    } else {
                        // 未进入交互状态，重置divider的translation（以防万一）
                        v.setTranslationX(0);
                        v.setTranslationY(0);
                    }
                    return true;
            }
            return false;
        }

        private void setInteractiveState(View v) {
            if (isHorizontal) {
                v.setBackgroundResource(R.drawable.divider_horizontal_interactive);
            } else {
                v.setBackgroundResource(R.drawable.divider_vertical_interactive);
            }
        }

        private void resetToDefaultState() {
            if (isHorizontal) {
                dividerHorizontal.setBackgroundResource(R.drawable.divider_horizontal_default);
            } else {
                dividerVertical.setBackgroundResource(R.drawable.divider_vertical_default);
            }
        }
    }
}
