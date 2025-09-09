package com.toddmo.bydmate.client.widgets;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;

import com.google.android.flexbox.FlexboxLayout;
import com.toddmo.bydmate.client.R;
import com.toddmo.bydmate.client.interfaces.InfoItem;
import com.toddmo.bydmate.client.widgets.infoitems.CpuInfoItem;
import com.toddmo.bydmate.client.widgets.infoitems.MemoryInfoItem;
import com.toddmo.bydmate.client.widgets.infoitems.TimeInfoItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * 信息栏组件
 * 支持InfoItem接口的插件化信息显示
 */
public class InfoBar extends LinearLayout {

    private FlexboxLayout infoContainer;
    private List<InfoItem> infoItems;
    private Handler handler;
    private Runnable updateRunnable;

    // 默认信息项目
    private TimeInfoItem timeItem;
    private CpuInfoItem cpuItem;
    private MemoryInfoItem memoryItem;

    public InfoBar(Context context) {
        super(context);
        initialize(context);
    }

    public InfoBar(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initialize(context);
    }

    private void initialize(Context context) {
        // 加载布局
        LayoutInflater.from(context).inflate(R.layout.view_info_bar, this, true);

        // 初始化视图
        infoContainer = findViewById(R.id.info_container);

        // 初始化信息项目列表
        infoItems = new ArrayList<>();

        // 创建默认信息项目
        timeItem = new TimeInfoItem();
        cpuItem = new CpuInfoItem();
        memoryItem = new MemoryInfoItem();

        // 添加默认项目
        addInfoItem(timeItem);
        addInfoItem(cpuItem);
        addInfoItem(memoryItem);

        // 开始更新信息
        startInfoUpdates();
    }

    /**
     * 添加信息项目
     */
    public void addInfoItem(InfoItem item) {
        if (item != null && !infoItems.contains(item)) {
            infoItems.add(item);
            refreshDisplay();
        }
    }

    /**
     * 移除信息项目
     */
    public void removeInfoItem(String itemId) {
        infoItems.removeIf(item -> item.getId().equals(itemId));
        refreshDisplay();
    }

    /**
     * 获取所有信息项目
     */
    public List<InfoItem> getInfoItems() {
        return new ArrayList<>(infoItems);
    }

    /**
     * 刷新显示
     */
    private void refreshDisplay() {
        // 清除现有视图
        infoContainer.removeAllViews();

        // 按优先级排序
        List<InfoItem> sortedItems = new ArrayList<>(infoItems);
        Collections.sort(sortedItems, new Comparator<InfoItem>() {
            @Override
            public int compare(InfoItem item1, InfoItem item2) {
                return Integer.compare(item2.getPriority(), item1.getPriority());
            }
        });

        // 添加启用的项目
        for (InfoItem item : sortedItems) {
            if (item.isEnabled()) {
                View itemView = item.getDisplayView(getContext());
                if (itemView != null) {
                    infoContainer.addView(itemView);
                }
            }
        }
    }

    /**
     * 开始信息更新
     */
    private void startInfoUpdates() {
        handler = new Handler(Looper.getMainLooper());
        updateRunnable = new Runnable() {
            @Override
            public void run() {
                updateInfo();
                handler.postDelayed(this, 1000); // 每秒更新一次
            }
        };
        handler.post(updateRunnable);
    }

    /**
     * 更新所有信息
     */
    private void updateInfo() {
        // 更新CPU使用率（模拟数据）
        int cpuUsage = (int) (Math.random() * 100);
        cpuItem.setCpuUsage(cpuUsage);

        // 更新内存使用率（模拟数据）
        int memoryUsage = (int) (Math.random() * 100);
        memoryItem.setMemoryUsage(memoryUsage);

        // 时间会自动更新（在TimeInfoItem内部处理）
    }

    /**
     * 设置信息栏的显示模式
     */
    public void setDisplayMode(boolean isHorizontal) {
        // 可以根据横屏/竖屏调整显示内容
        if (isHorizontal) {
            // 横屏模式：显示所有项目
            cpuItem.setEnabled(true);
            memoryItem.setEnabled(true);
        } else {
            // 竖屏模式：隐藏CPU和内存，节省空间
            cpuItem.setEnabled(false);
            memoryItem.setEnabled(false);
        }
        refreshDisplay();
    }

    /**
     * 获取CPU信息项目
     */
    public CpuInfoItem getCpuItem() {
        return cpuItem;
    }

    /**
     * 获取内存信息项目
     */
    public MemoryInfoItem getMemoryItem() {
        return memoryItem;
    }

    /**
     * 获取时间信息项目
     */
    public TimeInfoItem getTimeItem() {
        return timeItem;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        // 停止更新
        if (handler != null && updateRunnable != null) {
            handler.removeCallbacks(updateRunnable);
        }
    }
}