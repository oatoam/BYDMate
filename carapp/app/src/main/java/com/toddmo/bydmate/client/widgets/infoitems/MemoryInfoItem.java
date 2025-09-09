package com.toddmo.bydmate.client.widgets.infoitems;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.widget.TextView;

import com.toddmo.bydmate.client.interfaces.InfoItem;

/**
 * 内存使用率信息项目
 */
public class MemoryInfoItem implements InfoItem {

    private static final String ID = "memory";
    private static final String DISPLAY_NAME = "内存使用率";
    private static final int PRIORITY = 70;

    private boolean enabled = true;
    private TextView displayView;
    private int memoryUsage = 0;

    @Override
    public View getDisplayView(Context context) {
        if (displayView == null) {
            displayView = new TextView(context);
            displayView.setTextColor(context.getResources().getColor(android.R.color.white));
            displayView.setTextSize(12);
            displayView.setGravity(Gravity.CENTER);
            displayView.setPadding(6, 2, 6, 2);
            displayView.setOnClickListener(v -> onClick());
        }

        // 更新内存使用率显示
        updateMemoryDisplay();
        return displayView;
    }

    @Override
    public void onClick() {
        // 内存点击可以显示详细信息或打开系统内存管理
        // 这里暂时不实现具体功能
    }

    @Override
    public void onDataChanged(Object data) {
        if (data instanceof Integer) {
            this.memoryUsage = (Integer) data;
            updateMemoryDisplay();
        }
    }

    private void updateMemoryDisplay() {
        if (displayView != null) {
            displayView.setText("内存: " + memoryUsage + "%");
        }
    }

    /**
     * 获取当前内存使用率
     * 这里应该实现实际的内存使用率获取逻辑
     * 暂时返回模拟数据
     */
    public int getMemoryUsage() {
        // 实际实现中应该从系统获取真实的内存使用率
        // 这里返回模拟数据
        return memoryUsage;
    }

    /**
     * 设置内存使用率
     */
    public void setMemoryUsage(int memoryUsage) {
        this.memoryUsage = Math.max(0, Math.min(100, memoryUsage));
        onDataChanged(this.memoryUsage);
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getDisplayName() {
        return DISPLAY_NAME;
    }

    @Override
    public int getPriority() {
        return PRIORITY;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}