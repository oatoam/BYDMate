package com.toddmo.bydmate.client.widgets.infoitems;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.widget.TextView;

import com.toddmo.bydmate.client.interfaces.InfoItem;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * 时间信息项目
 */
public class TimeInfoItem implements InfoItem {

    private static final String ID = "time";
    private static final String DISPLAY_NAME = "时间";
    private static final int PRIORITY = 100;

    private boolean enabled = true;
    private TextView displayView;
    private SimpleDateFormat timeFormat;

    public TimeInfoItem() {
        timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
    }

    @Override
    public View getDisplayView(Context context) {
        if (displayView == null) {
            displayView = new TextView(context);
            displayView.setTextColor(context.getResources().getColor(android.R.color.white));
            displayView.setTextSize(14);
            displayView.setGravity(Gravity.CENTER);
            displayView.setPadding(8, 4, 8, 4);
            displayView.setOnClickListener(v -> onClick());
        }

        // 更新时间显示
        updateTime();
        return displayView;
    }

    @Override
    public void onClick() {
        // 时间点击可以切换显示格式或其他操作
        // 这里暂时不实现具体功能
    }

    @Override
    public void onDataChanged(Object data) {
        // 时间数据变化，更新显示
        updateTime();
    }

    private void updateTime() {
        if (displayView != null) {
            String currentTime = timeFormat.format(new Date());
            displayView.setText(currentTime);
        }
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