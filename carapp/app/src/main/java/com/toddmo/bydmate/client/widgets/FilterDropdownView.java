package com.toddmo.bydmate.client.widgets;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.toddmo.bydmate.client.R;

import java.util.ArrayList;
import java.util.List;

/**
 * 自定义下拉选择控件
 * 替代Spinner实现的下拉选择功能，使用内部弹窗显示
 */
public class FilterDropdownView extends LinearLayout {

    private static final String TAG = "FilterDropdownView";

    private TextView selectedTextView;
    private View dropdownArrow;
    private FrameLayout dropdownContainer;
    private LinearLayout dropdownPanel;
    private RecyclerView dropdownRecyclerView;
    private DropdownAdapter adapter;
    private View overlayView;

    private List<String> options = new ArrayList<>();
    private int selectedPosition = 0;
    private OnItemSelectedListener listener;

    public interface OnItemSelectedListener {
        void onItemSelected(int position, String item);
    }

    public FilterDropdownView(@NonNull Context context) {
        super(context);
        initialize(context);
    }

    public FilterDropdownView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initialize(context);
    }

    public FilterDropdownView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize(context);
    }

    private void initialize(Context context) {
        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER_VERTICAL);
        setBackgroundResource(R.drawable.dropdown_background);
        setPadding(12, 8, 12, 8);

        // 创建选中项显示文本
        selectedTextView = new TextView(context);
        selectedTextView.setTextSize(12);
        selectedTextView.setTextColor(Color.BLACK);
        selectedTextView.setLayoutParams(new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1));

        // 创建下拉箭头
        dropdownArrow = new View(context);
        dropdownArrow.setBackgroundResource(R.drawable.dropdown_arrow);
        LayoutParams arrowParams = new LayoutParams(12, 12);
        arrowParams.setMargins(8, 0, 0, 0);
        dropdownArrow.setLayoutParams(arrowParams);

        addView(selectedTextView);
        addView(dropdownArrow);

        // 设置点击事件
        setOnClickListener(v -> showDropdown());

        // 初始化内部弹窗
        initializeDropdownPanel(context);
    }

    private void initializeDropdownPanel(Context context) {
        // 初始化适配器
        adapter = new DropdownAdapter();
    }

    private void ensureDropdownContainer(Context context) {
        if (dropdownContainer != null) return;

        // 获取父容器
        ViewGroup parent = (ViewGroup) getParent();
        if (parent == null) return;

        // 查找或创建下拉容器
        int containerId = 0x7f0f0000; // 使用一个固定的ID值
        dropdownContainer = parent.findViewById(containerId);
        if (dropdownContainer == null) {
            dropdownContainer = new FrameLayout(context);
            dropdownContainer.setId(containerId);
            dropdownContainer.setBackgroundColor(Color.parseColor("#80000000"));
            dropdownContainer.setVisibility(View.GONE);

            // 创建遮罩层
            overlayView = new View(context);
            overlayView.setBackgroundColor(Color.TRANSPARENT);
            overlayView.setOnClickListener(v -> hideDropdown());
            dropdownContainer.addView(overlayView, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));

            // 创建下拉面板
            dropdownPanel = new LinearLayout(context);
            dropdownPanel.setOrientation(LinearLayout.VERTICAL);
            dropdownPanel.setBackgroundColor(Color.WHITE);
            dropdownPanel.setElevation(8);

            // 创建RecyclerView
            dropdownRecyclerView = new RecyclerView(context);
            dropdownRecyclerView.setLayoutManager(new LinearLayoutManager(context));
            dropdownRecyclerView.setAdapter(adapter);
            dropdownRecyclerView.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
            dropdownPanel.addView(dropdownRecyclerView);

            // 计算FilterDropdownView的位置
            int[] location = new int[2];
            getLocationOnScreen(location);
            int viewWidth = getWidth();
            int viewHeight = getHeight();

            FrameLayout.LayoutParams panelParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT);
            panelParams.leftMargin = location[0] + viewWidth / 2 - 100; // 居中显示，假设面板宽度约200dp
            panelParams.topMargin = location[1] + viewHeight + 10; // 在控件下方显示
            dropdownContainer.addView(dropdownPanel, panelParams);

            parent.addView(dropdownContainer);
        }
    }

    /**
     * 设置选项列表
     */
    public void setOptions(List<String> options) {
        this.options.clear();
        this.options.addAll(options);
        if (!options.isEmpty()) {
            selectedTextView.setText(options.get(0));
        }
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    /**
     * 设置选中项
     */
    public void setSelection(int position) {
        if (position >= 0 && position < options.size()) {
            selectedPosition = position;
            selectedTextView.setText(options.get(position));
        }
    }

    /**
     * 获取当前选中位置
     */
    public int getSelectedPosition() {
        return selectedPosition;
    }

    /**
     * 获取当前选中项
     */
    public String getSelectedItem() {
        if (selectedPosition >= 0 && selectedPosition < options.size()) {
            return options.get(selectedPosition);
        }
        return null;
    }

    /**
     * 设置选择监听器
     */
    public void setOnItemSelectedListener(OnItemSelectedListener listener) {
        this.listener = listener;
    }

    /**
     * 显示下拉列表
     */
    private void showDropdown() {
        if (!options.isEmpty()) {
            ensureDropdownContainer(getContext());
            if (dropdownContainer != null) {
                dropdownContainer.setVisibility(View.VISIBLE);
            }
        }
    }

    /**
     * 隐藏下拉列表
     */
    public void dismissDropdown() {
        if (dropdownContainer != null) {
            dropdownContainer.setVisibility(View.GONE);
        }
    }

    /**
     * 隐藏下拉列表
     */
    private void hideDropdown() {
        dismissDropdown();
    }

    /**
     * 下拉列表适配器
     */
    private class DropdownAdapter extends RecyclerView.Adapter<DropdownAdapter.ViewHolder> {

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_dropdown_option, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            holder.bind(options.get(position), position);
        }

        @Override
        public int getItemCount() {
            return options.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            private TextView optionText;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                optionText = itemView.findViewById(R.id.option_text);
            }

            void bind(String option, int position) {
                optionText.setText(option);
                optionText.setTextSize(12);
                optionText.setTextColor(Color.BLACK);
                optionText.setPadding(16, 12, 16, 12);

                // 设置选中状态样式
                if (position == selectedPosition) {
                    optionText.setBackgroundColor(Color.LTGRAY);
                } else {
                    optionText.setBackgroundColor(Color.WHITE);
                }

                itemView.setOnClickListener(v -> {
                    selectedPosition = position;
                    selectedTextView.setText(option);
                    hideDropdown();

                    if (listener != null) {
                        listener.onItemSelected(position, option);
                    }
                });
            }
        }
    }
}