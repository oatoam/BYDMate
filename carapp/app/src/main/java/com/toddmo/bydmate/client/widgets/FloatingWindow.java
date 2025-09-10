package com.toddmo.bydmate.client.widgets;

import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;

import com.toddmo.bydmate.client.R;

/**
 * 悬浮窗组件
 * 显示在屏幕底部，Z轴在所有UI顶部
 */
public class FloatingWindow {

    private static final String TAG = "FloatingWindow";

    private Context context;
    private WindowManager windowManager;
    private WindowManager.LayoutParams layoutParams;
    private View floatingView;
    private boolean isShowing = false;

    // 按钮点击监听器
    private OnButtonClickListener buttonClickListener;

    public interface OnButtonClickListener {
        void onBackClick();
        void onHomeClick();
        void onRecentClick();
    }

    public FloatingWindow(Context context) {
        this.context = context;
        initialize();
    }

    private void initialize() {
        // 检查悬浮窗权限
        if (!Settings.canDrawOverlays(context)) {
            Log.w(TAG, "No overlay permission");
            return;
        }

        windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);

        // 创建悬浮窗布局参数
        layoutParams = new WindowManager.LayoutParams();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            layoutParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            layoutParams.type = WindowManager.LayoutParams.TYPE_PHONE;
        }

        layoutParams.format = PixelFormat.RGBA_8888;
        layoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |
                            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS |
                            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;

        // 设置悬浮窗位置和大小
//        layoutParams.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        layoutParams.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT;
        layoutParams.height = getNavigationBarHeight();
        layoutParams.y = -getNavigationBarHeight();
        

        // 创建悬浮窗视图
        createFloatingView();
    }

    private void createFloatingView() {
        LayoutInflater inflater = LayoutInflater.from(context);
        floatingView = inflater.inflate(R.layout.view_floating_window, null);

        // 设置按钮点击监听器
        View backButton = floatingView.findViewById(R.id.back_button);
        View homeButton = floatingView.findViewById(R.id.home_button);
        View recentButton = floatingView.findViewById(R.id.recent_button);

        backButton.setOnClickListener(v -> {
            if (buttonClickListener != null) {
                buttonClickListener.onBackClick();
            }
        });

        homeButton.setOnClickListener(v -> {
            if (buttonClickListener != null) {
                buttonClickListener.onHomeClick();
            }
        });

        recentButton.setOnClickListener(v -> {
            if (buttonClickListener != null) {
                buttonClickListener.onRecentClick();
            }
        });
    }

    /**
     * 显示悬浮窗
     */
    public void show() {
        if (isShowing || windowManager == null || floatingView == null) {
            return;
        }

        try {
            windowManager.addView(floatingView, layoutParams);
            isShowing = true;
            Log.d(TAG, "Floating window shown");
        } catch (Exception e) {
            Log.e(TAG, "Failed to show floating window: " + e.getMessage());
        }
    }

    /**
     * 隐藏悬浮窗
     */
    public void hide() {
        if (!isShowing || windowManager == null || floatingView == null) {
            return;
        }

        try {
            windowManager.removeView(floatingView);
            isShowing = false;
            Log.d(TAG, "Floating window hidden");
        } catch (Exception e) {
            Log.e(TAG, "Failed to hide floating window: " + e.getMessage());
        }
    }

    /**
     * 设置按钮点击监听器
     */
    public void setOnButtonClickListener(OnButtonClickListener listener) {
        this.buttonClickListener = listener;
    }

    /**
     * 检查悬浮窗是否正在显示
     */
    public boolean isShowing() {
        return isShowing;
    }

    /**
     * 获取导航栏高度
     */
    private int getNavigationBarHeight() {
        int resourceId = context.getResources().getIdentifier("navigation_bar_height", "dimen", "android");
        if (resourceId > 0) {
            return context.getResources().getDimensionPixelSize(resourceId);
        }
        // 默认高度
        return (int) (48 * context.getResources().getDisplayMetrics().density);
    }

    /**
     * 设置悬浮窗颜色和透明度
     */
    public void setWindowStyle(int backgroundColor, float alpha) {
        if (floatingView != null) {
            floatingView.setBackgroundColor(backgroundColor);
            floatingView.setAlpha(alpha);
        }
    }

    /**
     * 销毁悬浮窗
     */
    public void destroy() {
        hide();
        context = null;
        windowManager = null;
        floatingView = null;
        buttonClickListener = null;
    }
}