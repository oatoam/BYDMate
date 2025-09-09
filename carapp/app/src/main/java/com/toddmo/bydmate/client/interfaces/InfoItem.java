package com.toddmo.bydmate.client.interfaces;

import android.content.Context;
import android.view.View;

/**
 * 信息项目接口
 * 定义信息栏中可显示项目的标准接口
 */
public interface InfoItem {

    /**
     * 获取显示视图
     * @param context Android上下文
     * @return 用于显示的View对象
     */
    View getDisplayView(Context context);

    /**
     * 点击响应
     * 当用户点击该信息项目时调用
     */
    void onClick();

    /**
     * 数据变化回调
     * 当相关数据发生变化时调用，用于更新显示内容
     * @param data 新的数据对象
     */
    void onDataChanged(Object data);

    /**
     * 获取项目标识符
     * 用于唯一标识该信息项目
     * @return 项目标识符
     */
    String getId();

    /**
     * 获取项目显示名称
     * @return 显示名称
     */
    String getDisplayName();

    /**
     * 获取项目优先级
     * 用于排序，数字越大优先级越高
     * @return 优先级值
     */
    int getPriority();

    /**
     * 是否启用该项目
     * @return true表示启用，false表示禁用
     */
    boolean isEnabled();

    /**
     * 设置启用状态
     * @param enabled 是否启用
     */
    void setEnabled(boolean enabled);
}