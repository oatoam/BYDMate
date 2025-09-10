package com.toddmo.bydmate.client.widgets;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.toddmo.bydmate.client.R;

import java.util.ArrayList;
import java.util.List;

/**
 * 应用列表适配器
 */
public class AppListAdapter extends RecyclerView.Adapter<AppListAdapter.AppViewHolder> {

    // 筛选类型枚举
    public enum FilterType {
        ALL("全部"),
        MAP("地图"),
        MUSIC("音乐");

        private final String displayName;

        FilterType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    private List<AppInfo> appList;
    private List<AppInfo> filteredList;
    private OnAppSelectedListener listener;
    private FilterType currentFilter = FilterType.ALL;
    private String searchQuery = "";

    public interface OnAppSelectedListener {
        void onAppSelected(AppInfo appInfo, boolean isSelected);
    }

    public AppListAdapter(List<AppInfo> appList, OnAppSelectedListener listener) {
        this.appList = new ArrayList<>(appList);
        this.filteredList = new ArrayList<>();
        this.listener = listener;
        applyFilters(); // 初始化时应用筛选
    }

    @NonNull
    @Override
    public AppViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_app_list, parent, false);
        return new AppViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AppViewHolder holder, int position) {
        AppInfo appInfo = filteredList.get(position);
        holder.bind(appInfo);
    }

    @Override
    public int getItemCount() {
        return filteredList.size();
    }

    /**
     * 设置筛选类型
     */
    public void setFilter(FilterType filterType) {
        this.currentFilter = filterType;
        applyFilters();
    }

    /**
     * 搜索应用列表
     */
    public void filter(String query) {
        this.searchQuery = query != null ? query.trim() : "";
        applyFilters();
    }

    /**
     * 应用所有筛选条件
     */
    private void applyFilters() {
        filteredList.clear();

        for (AppInfo app : appList) {
            if (matchesFilter(app) && matchesSearch(app)) {
                filteredList.add(app);
            }
        }

        notifyDataSetChanged();
    }

    /**
     * 检查应用是否匹配筛选条件
     */
    private boolean matchesFilter(AppInfo app) {
        if (currentFilter == FilterType.ALL) {
            return true;
        }

        String appName = app.getAppName().toLowerCase();
        String packageName = app.getPackageName().toLowerCase();

        switch (currentFilter) {
            case MAP:
                return appName.contains("地图") || appName.contains("map") ||
                       packageName.contains("map") || packageName.contains("location") ||
                       packageName.contains("navigation");
            case MUSIC:
                return appName.contains("音乐") || appName.contains("music") ||
                       appName.contains("播放器") || appName.contains("player") ||
                       packageName.contains("music") || packageName.contains("audio");
            default:
                return true;
        }
    }

    /**
     * 检查应用是否匹配搜索条件
     */
    private boolean matchesSearch(AppInfo app) {
        if (searchQuery.isEmpty()) {
            return true;
        }

        String lowerQuery = searchQuery.toLowerCase();
        String appName = app.getAppName().toLowerCase();
        String packageName = app.getPackageName().toLowerCase();

        return appName.contains(lowerQuery) || packageName.contains(lowerQuery);
    }

    /**
     * 获取选中的应用
     */
    public AppInfo getSelectedApp() {
        for (AppInfo app : appList) {
            if (app.isSelected()) {
                return app;
            }
        }
        return null;
    }

    /**
     * 更新数据
     */
    public void updateData(List<AppInfo> newAppList) {
        this.appList.clear();
        this.appList.addAll(newAppList);
        applyFilters();
    }

    /**
     * 清除所有选择
     */
    public void clearSelection() {
        for (AppInfo app : appList) {
            app.setSelected(false);
        }
        notifyDataSetChanged();
    }

    class AppViewHolder extends RecyclerView.ViewHolder {
        private ImageView appIcon;
        private TextView appName;
        private CheckBox checkBox;

        AppViewHolder(@NonNull View itemView) {
            super(itemView);
            appIcon = itemView.findViewById(R.id.app_icon);
            appName = itemView.findViewById(R.id.app_name);
            checkBox = itemView.findViewById(R.id.app_checkbox);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    AppInfo appInfo = filteredList.get(position);
                    // 清除其他选择
                    clearSelection();
                    // 选择当前应用
                    appInfo.setSelected(!appInfo.isSelected());
                    checkBox.setChecked(appInfo.isSelected());

                    if (listener != null) {
                        listener.onAppSelected(appInfo, appInfo.isSelected());
                    }
                }
            });

            checkBox.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    AppInfo appInfo = filteredList.get(position);
                    // 清除其他选择
                    clearSelection();
                    // 选择当前应用
                    appInfo.setSelected(checkBox.isChecked());

                    if (listener != null) {
                        listener.onAppSelected(appInfo, appInfo.isSelected());
                    }
                }
            });
        }

        void bind(AppInfo appInfo) {
            appIcon.setImageDrawable(appInfo.getAppIcon());
            appName.setText(appInfo.getAppName());
            checkBox.setChecked(appInfo.isSelected());
        }
    }
}