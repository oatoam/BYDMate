package com.toddmo.bydmate.client.widgets.infoitems;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.widget.TextView;

import com.toddmo.bydmate.client.interfaces.InfoItem;

/**
 * CPU使用率信息项目
 */
public class CpuInfoItem implements InfoItem {

    private static final String ID = "cpu";
    private static final String DISPLAY_NAME = "CPU使用率";
    private static final int PRIORITY = 80;

    private boolean enabled = true;
    private TextView displayView;
    private int cpuUsage = 0;

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

        // 更新CPU使用率显示
        updateCpuDisplay();
        return displayView;
    }

    @Override
    public void onClick() {
        // CPU点击可以显示详细信息或打开系统监控
        // 这里暂时不实现具体功能
    }

    @Override
    public void onDataChanged(Object data) {
        if (data instanceof Integer) {
            this.cpuUsage = (Integer) data;
            updateCpuDisplay();
        }
    }

    private void updateCpuDisplay() {
        if (displayView != null) {
            displayView.setText("CPU: " + cpuUsage + "%");
        }
    }

    /**
     * 获取当前CPU使用率
     * 这里应该实现实际的CPU使用率获取逻辑
     * 暂时返回模拟数据
     */
    public int getCpuUsage() {
        // 实际实现中应该从系统获取真实的CPU使用率
        // 这里返回模拟数据
        return cpuUsage;
    }

    /**
     * 设置CPU使用率
     */
    public void setCpuUsage(int cpuUsage) {
        this.cpuUsage = Math.max(0, Math.min(100, cpuUsage));
        onDataChanged(this.cpuUsage);
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