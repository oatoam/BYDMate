package com.toddmo.bydmate.client.widgets;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.toddmo.bydmate.client.R;
import com.toddmo.bydmate.client.utils.KLog;

import java.util.ArrayList;
import java.util.List;

/**
 * 应用选择视图组件
 * 替代AppSelectorDialog的View实现
 */
public class AppSelectorView extends FrameLayout {

    private static final String TAG = "AppSelectorView";

    private Context context;
    private OnAppSelectedListener listener;
    private List<AppInfo> appList;
    private AppListAdapter adapter;
    private Spinner filterSpinner;
    private AppSelectorConfigManager configManager;

    public interface OnAppSelectedListener {
        void onAppSelected(String packageName);
        void onCancel();
    }

    public AppSelectorView(@NonNull Context context) {
        super(context);
        initialize(context);
    }

    public AppSelectorView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initialize(context);
    }

    public AppSelectorView(@NonNull Context context, String instanceId) {
        super(context);
        initialize(context, instanceId);
    }

    public AppSelectorView(@NonNull Context context, @Nullable AttributeSet attrs, String instanceId) {
        super(context, attrs);
        initialize(context, instanceId);
    }

    private void initialize(Context context) {
        initialize(context, "default");
    }

    private void initialize(Context context, String instanceId) {
        this.context = context;
        this.appList = new ArrayList<>();

        // 初始化配置管理器
        configManager = new AppSelectorConfigManager(context, instanceId);

        // 加载布局
        inflate(context, R.layout.view_app_selector, this);

        // 设置默认不可见
        setVisibility(GONE);

        initializeViews();
        loadInstalledApps();
        loadSavedConfiguration();
    }

    private void initializeViews() {
        // 筛选下拉框
        filterSpinner = findViewById(R.id.filter_spinner);

        // 设置Spinner的数据源
        String[] filterOptions = {
            AppListAdapter.FilterType.ALL.getDisplayName(),
            AppListAdapter.FilterType.MAP.getDisplayName(),
            AppListAdapter.FilterType.MUSIC.getDisplayName()
        };

        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(context,
            android.R.layout.simple_spinner_item, filterOptions);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        filterSpinner.setAdapter(spinnerAdapter);

        // 防止Spinner展开时退出全屏模式
        try {
            java.lang.reflect.Field popupField = android.widget.Spinner.class.getDeclaredField("mPopup");
            popupField.setAccessible(true);
            Object popup = popupField.get(filterSpinner);
            if (popup instanceof android.widget.ListPopupWindow) {
                android.widget.ListPopupWindow listPopupWindow = (android.widget.ListPopupWindow) popup;
                listPopupWindow.setModal(false);
                listPopupWindow.setInputMethodMode(android.widget.PopupWindow.INPUT_METHOD_NOT_NEEDED);
            }
        } catch (Exception e) {
            android.util.Log.w(TAG, "Failed to configure spinner popup window: " + e.getMessage());
        }

        // 设置默认选择为"不筛选"
        filterSpinner.setSelection(0);

        // 设置选择监听器
        filterSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                AppListAdapter.FilterType filterType;
                switch (position) {
                    case 1:
                        filterType = AppListAdapter.FilterType.MAP;
                        break;
                    case 2:
                        filterType = AppListAdapter.FilterType.MUSIC;
                        break;
                    default:
                        filterType = AppListAdapter.FilterType.ALL;
                        break;
                }
                AppSelectorView.this.adapter.setFilter(filterType);
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
                // 默认选择不筛选
                AppSelectorView.this.adapter.setFilter(AppListAdapter.FilterType.ALL);
            }
        });

        // 搜索框
        EditText searchEditText = findViewById(R.id.search_edit_text);
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.filter(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // RecyclerView
        RecyclerView recyclerView = findViewById(R.id.app_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(context));
        adapter = new AppListAdapter(appList, (appInfo, isSelected) -> {
            // 处理应用选择 - 清除其他选择，确保单选
            if (isSelected) {
                adapter.clearSelection();
                appInfo.setSelected(true);
            }
        });
        recyclerView.setAdapter(adapter);

        // 添加调试日志：检查RecyclerView的布局参数
        android.view.ViewGroup.LayoutParams layoutParams = recyclerView.getLayoutParams();
        android.util.Log.d(TAG, "RecyclerView height: " + layoutParams.height + "px");
        android.util.Log.d(TAG, "RecyclerView measured height: " + recyclerView.getMeasuredHeight() + "px");
        android.util.Log.d(TAG, "App list size: " + appList.size());
        android.util.Log.d(TAG, "Filtered list size: " + adapter.getItemCount());

        // 按钮
        Button cancelButton = findViewById(R.id.cancel_button);
        Button confirmButton = findViewById(R.id.confirm_button);

        cancelButton.setOnClickListener(v -> {
            hide();
            if (listener != null) {
                listener.onCancel();
            }
        });

        confirmButton.setOnClickListener(v -> {
            AppInfo selectedApp = adapter.getSelectedApp();
            hide();

            // 更新配置管理器
            String selectedPackage = selectedApp != null ? selectedApp.getPackageName() : null;
            configManager.setSelectedAppPackage(selectedPackage);

            if (listener != null) {
                if (selectedApp != null) {
                    listener.onAppSelected(selectedApp.getPackageName());
                } else {
                    listener.onAppSelected(null);
                }
            }
        });
    }

    /**
     * 显示应用选择器
     */
    public void show() {
        setVisibility(VISIBLE);
        // 确保视图获得焦点
        requestFocus();
    }

    /**
     * 隐藏应用选择器
     */
    public void hide() {
        setVisibility(GONE);
    }

    /**
     * 设置应用选择监听器
     */
    public void setOnAppSelectedListener(OnAppSelectedListener listener) {
        this.listener = listener;
    }

    /**
     * 加载已安装的应用
     */
    private void loadInstalledApps() {
        PackageManager packageManager = context.getPackageManager();
        Intent intent = new Intent(Intent.ACTION_MAIN, null);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);

        List<ResolveInfo> resolveInfos = packageManager.queryIntentActivities(intent, 0);
        appList.clear();

        android.util.Log.d("AppSelectorView", "Found " + resolveInfos.size() + " apps");

        for (ResolveInfo resolveInfo : resolveInfos) {
            String packageName = resolveInfo.activityInfo.packageName;
            String appName = resolveInfo.loadLabel(packageManager).toString();
            android.graphics.drawable.Drawable appIcon = resolveInfo.loadIcon(packageManager);

            android.util.Log.d("AppSelectorView", "App: " + appName + " (" + packageName + ")");

            AppInfo appInfo = new AppInfo(packageName, appName, appIcon);
            appList.add(appInfo);
        }

        android.util.Log.d("AppSelectorView", "Total apps loaded: " + appList.size());

        if (adapter != null) {
            // 更新适配器数据
            adapter.updateData(appList);
            android.util.Log.d("AppSelectorView", "Adapter updated, item count: " + adapter.getItemCount());
        }
    }

    /**
     * 加载已保存的配置并设置默认选择
     */
    private void loadSavedConfiguration() {
        String savedPackage = configManager.getSelectedAppPackage();
        if (savedPackage != null && !savedPackage.isEmpty()) {
            KLog.d(TAG + " Loading saved configuration: " + savedPackage);
            // 在应用列表中查找对应的应用并设置为选中状态
            for (AppInfo appInfo : appList) {
                if (savedPackage.equals(appInfo.getPackageName())) {
                    appInfo.setSelected(true);
                    if (adapter != null) {
                        adapter.notifyDataSetChanged();
                    }
                    KLog.d(TAG + " Set saved app as selected: " + savedPackage);
                    break;
                }
            }
        } else {
            KLog.d(TAG + " No saved configuration found");
        }
    }

    /**
     * 获取配置管理器
     */
    public AppSelectorConfigManager getConfigManager() {
        return configManager;
    }

    /**
     * 设置配置变更监听器
     */
    public void setOnConfigChangedListener(AppSelectorConfigManager.OnConfigChangedListener listener) {
        if (configManager != null) {
            configManager.addOnConfigChangedListener(listener);
        }
    }

    /**
     * 移除配置变更监听器
     */
    public void removeOnConfigChangedListener(AppSelectorConfigManager.OnConfigChangedListener listener) {
        if (configManager != null) {
            configManager.removeOnConfigChangedListener(listener);
        }
    }
}