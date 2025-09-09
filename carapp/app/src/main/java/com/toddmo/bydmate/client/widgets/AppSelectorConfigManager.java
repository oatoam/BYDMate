package com.toddmo.bydmate.client.widgets;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.toddmo.bydmate.client.utils.KLog;

import java.util.ArrayList;
import java.util.List;

/**
 * AppSelector配置管理器
 * 负责管理应用选择器的配置，包括SharedPreferences的读写和配置变更通知
 */
public class AppSelectorConfigManager {

    private static final String TAG = "AppSelectorConfigManager";

    // SharedPreferences keys
    private static final String PREF_KEY_SELECTED_APP_PACKAGE = "pip_selected_app_package";
    private static final String PREF_KEY_APP_LIST_VERSION = "pip_app_list_version";

    // 配置变更监听器
    public interface OnConfigChangedListener {
        void onSelectedAppChanged(String packageName);
    }

    private final SharedPreferences sharedPreferences;
    private final List<OnConfigChangedListener> listeners = new ArrayList<>();
    private final String instanceId;
    private String currentSelectedPackage;

    public AppSelectorConfigManager(Context context) {
        this(context, "default");
    }

    public AppSelectorConfigManager(Context context, String instanceId) {
        this.sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        this.instanceId = instanceId != null ? instanceId : "default";
        this.currentSelectedPackage = loadSelectedAppPackage();
        KLog.d(TAG + " initialized with instanceId: " + this.instanceId + ", selected package: " + currentSelectedPackage);
    }

    /**
     * 获取当前选中的应用包名
     */
    public String getSelectedAppPackage() {
        return currentSelectedPackage;
    }

    /**
     * 设置选中的应用包名
     */
    public void setSelectedAppPackage(String packageName) {
        if ((currentSelectedPackage == null && packageName == null) ||
            (currentSelectedPackage != null && currentSelectedPackage.equals(packageName))) {
            return; // 没有变化
        }

        KLog.d(TAG + " Setting " + instanceId + " selected app package: " + packageName);
        currentSelectedPackage = packageName;
        saveSelectedAppPackage(packageName);
        notifyConfigChanged(packageName);
    }

    /**
     * 添加配置变更监听器
     */
    public void addOnConfigChangedListener(OnConfigChangedListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    /**
     * 移除配置变更监听器
     */
    public void removeOnConfigChangedListener(OnConfigChangedListener listener) {
        listeners.remove(listener);
    }

    /**
     * 清除所有监听器
     */
    public void clearAllListeners() {
        listeners.clear();
    }

    /**
     * 获取当前实例的SharedPreferences key
     */
    private String getInstanceKey() {
        return PREF_KEY_SELECTED_APP_PACKAGE + "_" + instanceId;
    }

    /**
     * 从SharedPreferences加载选中的应用包名
     */
    private String loadSelectedAppPackage() {
        String key = getInstanceKey();
        String packageName = sharedPreferences.getString(key, null);
        KLog.d(TAG + " Loaded selected app package for instance " + instanceId + ": " + packageName);
        return packageName;
    }

    /**
     * 保存选中的应用包名到SharedPreferences
     */
    private void saveSelectedAppPackage(String packageName) {
        String key = getInstanceKey();
        SharedPreferences.Editor editor = sharedPreferences.edit();
        if (packageName != null && !packageName.isEmpty()) {
            editor.putString(key, packageName);
        } else {
            editor.remove(key);
        }
        editor.apply();
        KLog.d(TAG + " Saved selected app package for instance " + instanceId + ": " + packageName);
    }

    /**
     * 通知所有监听器配置已变更
     */
    private void notifyConfigChanged(String packageName) {
        for (OnConfigChangedListener listener : listeners) {
            try {
                listener.onSelectedAppChanged(packageName);
            } catch (Exception e) {
                KLog.e(TAG + " Error notifying config change listener: " + e.getMessage());
            }
        }
    }

    /**
     * 检查是否有已保存的应用配置
     */
    public boolean hasSavedAppConfig() {
        return currentSelectedPackage != null && !currentSelectedPackage.isEmpty();
    }

    /**
     * 清除所有配置
     */
    public void clearAllConfig() {
        KLog.d(TAG + " Clearing all configuration for instance: " + instanceId);
        currentSelectedPackage = null;
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(getInstanceKey());
        editor.remove(PREF_KEY_APP_LIST_VERSION + "_" + instanceId);
        editor.apply();
        notifyConfigChanged(null);
    }
}