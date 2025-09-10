package com.toddmo.bydmate.client.widgets;

import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.graphics.SurfaceTexture;
import android.view.TextureView;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.toddmo.bydmate.client.R;
import com.toddmo.bydmate.client.helper.AdbHelper;
import com.toddmo.bydmate.client.helper.TouchEventHandler;
import com.toddmo.bydmate.client.utils.KLog;

/**
 * 应用画中画视图组件
 * 负责管理画中画应用的启动、采集和显示
 *
 * 实例ID分配规则：
 * 1. XML属性指定（最高优先级）：通过 app:pipInstanceId="xxx" 指定
 * 2. 自动分配（默认行为）：
 *    - 第一个创建的实例：主PIPView (main_pip)
 *    - 第二个创建的实例：副PIPView (secondary_pip)
 *    - 后续实例：自动生成 (pip_3, pip_4, ...)
 *
 * XML使用示例：
 * <com.toddmo.bydmate.client.widgets.ApplicationPIPView
 *     android:layout_width="match_parent"
 *     android:layout_height="match_parent"
 *     app:pipInstanceId="main_pip" />
 *
 * <com.toddmo.bydmate.client.widgets.ApplicationPIPView
 *     android:layout_width="match_parent"
 *     android:layout_height="match_parent"
 *     app:pipInstanceId="secondary_pip" />
 *
 * 代码使用示例：
 * // 自动分配（推荐）
 * ApplicationPIPView pip1 = new ApplicationPIPView(context); // main_pip
 * ApplicationPIPView pip2 = new ApplicationPIPView(context); // secondary_pip
 *
 * // 显式创建
 * ApplicationPIPView mainPIP = ApplicationPIPView.createMainPIPView(context);
 * ApplicationPIPView secondaryPIP = ApplicationPIPView.createSecondaryPIPView(context);
 *
 * // 手动指定实例ID
 * ApplicationPIPView customPIP = new ApplicationPIPView(context, "custom_pip");
 *
 * 配置存储：
 * 每个实例都会使用独立的SharedPreferences key：
 * - 主PIPView: pip_selected_app_package_main_pip
 * - 副PIPView: pip_selected_app_package_secondary_pip
 * - 自定义: pip_selected_app_package_{instanceId}
 */
public class ApplicationPIPView extends FrameLayout {

    private static final String TAG = "ApplicationPIPView";

    // 预定义实例ID
    public static final String INSTANCE_MAIN = "main_pip";
    public static final String INSTANCE_SECONDARY = "secondary_pip";

    // 实例计数器，用于自动分配实例ID
    private static int instanceCounter = 0;

    // UI组件
    private TextureView mSurfaceView;
    private View configButton;
    private AppSelectorView appSelectorView;

    // 实例唯一标识
    private final String instanceId;

    // ADB相关
    private AdbHelper adbHelper;
    private AdbHelper.ADBCommandThread screenshotThread;
    private boolean isAdbConnected = false;

    // 应用管理
    private String currentPackageName;
    private boolean isAppRunning = false;

    // 监听器
    private OnAppSelectedListener appSelectedListener;
    private OnPermissionRequiredListener permissionRequiredListener;

    private VirtualDisplay mVirtualDisplay = null;

    // 配置管理器
    private AppSelectorConfigManager configManager;

    // 触摸事件处理器
    private TouchEventHandler touchEventHandler;

    // VirtualDisplay ID
    private int displayId = -1;

    // 配置
    private int mDisplayWidth = 720;
    private int mDisplayHeight = 1280;
    private int mDisplayDensity = 320;

    public interface OnAppSelectedListener {
        void onAppSelected(String packageName);
    }

    public interface OnPermissionRequiredListener {
        void onPermissionRequired();
    }

    public ApplicationPIPView(@NonNull Context context) {
        super(context);
        this.instanceId = getNextInstanceId();
        initialize(context);
    }

    public ApplicationPIPView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        this.instanceId = getInstanceIdFromAttrs(context, attrs);
        initialize(context);
    }

    public ApplicationPIPView(@NonNull Context context, String instanceId) {
        super(context);
        this.instanceId = instanceId != null ? instanceId : INSTANCE_MAIN;
        initialize(context);
    }

    public ApplicationPIPView(@NonNull Context context, @Nullable AttributeSet attrs, String instanceId) {
        super(context, attrs);
        this.instanceId = instanceId != null ? instanceId : INSTANCE_MAIN;
        initialize(context);
    }

    private void initialize(Context context) {
        // 初始化视图
        inflate(context, R.layout.view_application_pip, this);

        mSurfaceView = findViewById(R.id.surface_view);
        configButton = findViewById(R.id.config_button);

        // 创建并添加应用选择器视图
        appSelectorView = new AppSelectorView(context, instanceId);
        addView(appSelectorView);

        // 获取配置管理器并设置监听器
        configManager = appSelectorView.getConfigManager();
        if (configManager != null) {
            configManager.addOnConfigChangedListener(packageName -> {
                KLog.d(TAG + " [" + instanceId + "] Configuration changed, selected app: " + packageName);
                // 当配置发生变化时，可以在这里执行相应的逻辑
            });
        }

        mSurfaceView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surfaceTexture, int width, int height) {
                KLog.d("onSurfaceTextureAvailable");
                if (mDisplayHeight == height && mDisplayWidth == width && mVirtualDisplay != null) {
                    return;
                }
                KLog.d(String.format("onSurfaceTextureAvailable width %d -> %d height %d -> %d",
                        mDisplayWidth, width, mDisplayHeight, height));
                mDisplayWidth = width;
                mDisplayHeight = height;
                if (mVirtualDisplay != null) {
                    mVirtualDisplay.release();
                }

                createVirtualDisplayWithSurfaceTexture(surfaceTexture);
            }

            @Override
            public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surfaceTexture, int width, int height) {
                KLog.d("onSurfaceTextureSizeChanged");
                if (mDisplayHeight == height && mDisplayWidth == width && mVirtualDisplay != null) {
                    return;
                }
                KLog.d(String.format("onSurfaceTextureSizeChanged width %d -> %d height %d -> %d",
                        mDisplayWidth, width, mDisplayHeight, height));
                mDisplayWidth = width;
                mDisplayHeight = height;
                if (mVirtualDisplay != null) {
                    mVirtualDisplay.release();
                }

                createVirtualDisplayWithSurfaceTexture(surfaceTexture);
            }

            @Override
            public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surfaceTexture) {
                KLog.d("onSurfaceTextureDestroyed");
                if (mVirtualDisplay != null) {
                    mVirtualDisplay.release();
                }
                return true;
            }

            @Override
            public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surfaceTexture) {
                // Surface texture updated
            }
        });

        // Texture模式下不需要ADB连接
        KLog.d(TAG + " Texture mode initialized");

        // 获取屏幕参数
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        mDisplayWidth = metrics.widthPixels / 2;  // 默认使用屏幕宽度的一半
        mDisplayHeight = metrics.heightPixels / 2; // 默认使用屏幕高度的一半
        mDisplayDensity = metrics.densityDpi;

        // 初始化触摸事件处理器
        touchEventHandler = new TouchEventHandler();

        // 设置TextureView触摸监听器
        mSurfaceView.setOnTouchListener((v, event) -> {
            if (touchEventHandler != null) {
                return touchEventHandler.handleTouchEvent(v, event);
            }
            return false;
        });

        // 添加布局变化监听器，确保坐标转换参数始终正确
        mSurfaceView.addOnLayoutChangeListener((v, left, top, right, bottom,
                                              oldLeft, oldTop, oldRight, oldBottom) -> {
            if (touchEventHandler != null && displayId != -1 &&
                (left != oldLeft || top != oldTop || (right - left) != (oldRight - oldLeft) ||
                 (bottom - top) != (oldBottom - oldTop))) {
                // 位置或尺寸发生变化，更新坐标转换参数
                touchEventHandler.setCoordinateTransform(
                    left, top, mDisplayWidth, mDisplayHeight,
                    mDisplayWidth, mDisplayHeight);
                KLog.d(TAG + " Updated coordinate transform due to layout change: " +
                       "position(" + left + "," + top + ") size(" + mDisplayWidth + "x" + mDisplayHeight + ")");
            }
        });

        // 设置配置按钮点击监听
        configButton.setOnClickListener(v -> showAppSelector());

        // 设置配置按钮长按监听 - 重置PIPView、销毁和重启virtualdisplay、重启应用
        configButton.setOnLongClickListener(v -> {
            KLog.d(TAG + " [" + instanceId + "] Long press detected on config button, resetting PIP view");
            resetPIPView();
            return true; // 消费长按事件
        });

        KLog.d(TAG + " initialized with instanceId: " + instanceId);
    }

    /**
     * 显示应用选择器
     */
    private void showAppSelector() {
        appSelectorView.setOnAppSelectedListener(new AppSelectorView.OnAppSelectedListener() {
            @Override
            public void onAppSelected(String packageName) {
                if (packageName != null) {
                    startAppOnDisplay(packageName, displayId);
                    if (appSelectedListener != null) {
                        appSelectedListener.onAppSelected(packageName);
                    }
                }
            }

            @Override
            public void onCancel() {
                // 用户取消选择
            }
        });
        appSelectorView.show();
    }

    /**
     * 启动应用
     */
    public void startApp(String packageName) {
        if (packageName == null || packageName.isEmpty()) {
            KLog.w(TAG + " Package name is null or empty");
            showError("应用包名无效");
            return;
        }

        // 检查应用是否存在
        if (!isAppInstalled(packageName)) {
            KLog.e(TAG + " App not installed: " + packageName);
            showError("应用未安装");
            return;
        }

        PackageManager packageManager = getContext().getPackageManager();
        Intent intent = packageManager.getLaunchIntentForPackage(packageName);
        String action = intent.getAction();
        String component = intent.getComponent().toString();
        String componentName = intent.getComponent().getPackageName() +
                    "/" + intent.getComponent().getClassName();

        KLog.d("action " + action);
        KLog.d("component " + component);
        KLog.d("componentName " + componentName);

        // 停止当前应用
        stopApp();

        currentPackageName = packageName;
        isAppRunning = true;

        // 配置已通过AppSelectorView保存，无需额外操作
        KLog.d(TAG + " [" + instanceId + "] App configuration managed by AppSelectorView");

        // Texture模式下不需要ADB连接检查
        if (mVirtualDisplay == null) {
            KLog.e(TAG + " VirtualDisplay is null, cannot start app");
            showError("虚拟显示未初始化");
            stopApp();
            return;
        }

        startAppUsingAdb(componentName, displayId);
    }

    /**
     * 检查应用是否已安装
     */
    private boolean isAppInstalled(String packageName) {
        try {
            getContext().getPackageManager().getPackageInfo(packageName, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    /**
     * 显示错误信息
     */
    private void showError(String message) {
        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
    }

    /**
     * 停止应用
     */
    public void stopApp() {
        if (!isAppRunning) {
            return;
        }

        isAppRunning = false;
        currentPackageName = null;

        // Texture模式下不需要停止ADB截图线程

//        // 停止虚拟显示
//        if (virtualDisplay != null) {
//            virtualDisplay.release();
//            virtualDisplay = null;
//        }

        KLog.d(TAG + " App stopped");
    }

    /**
     * 在新的display上重启当前应用（不关闭原应用）
     */
    private void restartAppOnNewDisplay(int displayId) {
        if (currentPackageName == null || currentPackageName.isEmpty()) {
            KLog.w(TAG + " No current app to restart");
            return;
        }

        KLog.d(TAG + " Restarting app " + currentPackageName + " on display " + displayId);

        startAppUsingAdb(currentPackageName, displayId);
    }

    /**
     * 在指定display上启动应用（用于自动启动已配置的应用）
     */
    private void startAppOnDisplay(String packageName, int displayId) {
        if (packageName == null || packageName.isEmpty()) {
            KLog.w(TAG + " Package name is null or empty");
            return;
        }

        // 检查应用是否存在
        if (!isAppInstalled(packageName)) {
            KLog.e(TAG + " App not installed: " + packageName);
            return;
        }

        // 设置应用状态
        currentPackageName = packageName;
        isAppRunning = true;

        KLog.d(TAG + " Auto-starting app " + packageName + " on display " + displayId);
        startAppUsingAdb(packageName, displayId);
    }

    void startAppUsingAdb(String packageName, int displayid) {
        // 使用ADB命令在指定display上启动应用

        PackageManager packageManager = getContext().getPackageManager();
        Intent intent = packageManager.getLaunchIntentForPackage(packageName);
        String action = intent.getAction();
        String component = intent.getComponent().toString();
        String componentName = intent.getComponent().getPackageName() +
                "/" + intent.getComponent().getClassName();

        // TODO: don't go back to main activity if the app was running.

        String command = String.format("am start --display %d %s", displayid, componentName);
        KLog.d("start app using ADB: " + command);

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    AdbHelper.getInstance().executeAsync(command);
                    KLog.d(TAG + " Successfully auto-started app on display");
                } catch (Exception e) {
                    KLog.e(TAG + " Failed to auto-start app on display: " + e.getMessage());
                    // 重置状态
                    isAppRunning = false;
                    currentPackageName = null;
                }
            }
        }).start();
    }

    /**
     * 使用SurfaceTexture创建虚拟显示
     */
    private int createVirtualDisplayWithSurfaceTexture(SurfaceTexture surfaceTexture) {
        try {
            // 获取DisplayManager
            DisplayManager displayManager = (DisplayManager) getContext()
                .getSystemService(Context.DISPLAY_SERVICE);

            int flag = 0;
            flag = DisplayManager.VIRTUAL_DISPLAY_FLAG_PRESENTATION; // DYZM

            // 从SurfaceTexture创建Surface
            android.view.Surface surface = new android.view.Surface(surfaceTexture);

            KLog.d(String.format("mDisplayWidth %d mDisplayHeight %d mDisplayDensity %d",
                    mDisplayWidth, mDisplayHeight, mDisplayDensity));

            // 创建虚拟显示
            mVirtualDisplay = displayManager.createVirtualDisplay(
                "ApplicationPIP",
                    mDisplayWidth,
                    mDisplayHeight,
                    mDisplayDensity,
                    surface,
                    flag);

            if (mVirtualDisplay != null) {
                displayId = mVirtualDisplay.getDisplay().getDisplayId();
                KLog.d(TAG + " Virtual display created successfully with surface texture id = " + displayId);

                // 设置触摸事件处理器的displayId
                if (touchEventHandler != null) {
                    touchEventHandler.setDisplayId(displayId);
                    // 设置坐标转换参数，包含位置偏移
                    touchEventHandler.setCoordinateTransform(
                        mSurfaceView.getLeft(), mSurfaceView.getTop(),
                        mDisplayWidth, mDisplayHeight,
                        mDisplayWidth, mDisplayHeight);
                    KLog.d(TAG + " Touch event handler configured for display " + displayId +
                           " at position (" + mSurfaceView.getLeft() + "," + mSurfaceView.getTop() + ")");
                }

                // 如果之前有应用在运行，自动重启到新的display
                if (isAppRunning && currentPackageName != null && !currentPackageName.isEmpty()) {
                    KLog.d(TAG + " Auto-restarting app " + currentPackageName + " on new display " + displayId);
                    restartAppOnNewDisplay(displayId);
                } else {
                    // 如果没有应用在运行，检查是否有已保存的应用配置需要启动
                    if (configManager != null && configManager.hasSavedAppConfig()) {
                        String savedPackageName = configManager.getSelectedAppPackage();
                        if (savedPackageName != null && !savedPackageName.isEmpty()) {
                            KLog.d(TAG + " [" + instanceId + "] Auto-starting saved app " + savedPackageName + " on new display " + displayId);
                            startAppOnDisplay(savedPackageName, displayId);
                        }
                    }
                }

                return displayId;
            }

            KLog.e(TAG + " Failed to create virtual display");
        } catch (Exception e) {
            KLog.e(TAG + " Error creating virtual display with surface texture: " + e.getMessage());
        }

        return -1;
    }

    /**
     * 设置MediaProjection（兼容性方法，现在使用Texture）
     */
    public void setMediaProjection(MediaProjection mediaProjection) {
        // 现在使用Texture，不再需要MediaProjection
        KLog.d(TAG + " MediaProjection set (Texture mode)");
    }

    /**
     * 设置应用选择监听器
     */
    public void setOnAppSelectedListener(OnAppSelectedListener listener) {
        this.appSelectedListener = listener;
    }

    /**
     * 设置权限请求监听器
     */
    public void setOnPermissionRequiredListener(OnPermissionRequiredListener listener) {
        this.permissionRequiredListener = listener;
    }

    /**
     * 获取当前运行的应用包名
     */
    public String getCurrentAppPackage() {
        return currentPackageName;
    }

    /**
     * 从XML属性中获取实例ID
     */
    private String getInstanceIdFromAttrs(Context context, AttributeSet attrs) {
        if (attrs != null) {
            android.content.res.TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.ApplicationPIPView);
            try {
                String pipInstanceId = typedArray.getString(R.styleable.ApplicationPIPView_pipInstanceId);
                if (pipInstanceId != null && !pipInstanceId.trim().isEmpty()) {
                    KLog.d(TAG + " Using XML specified instanceId: " + pipInstanceId);
                    return pipInstanceId;
                }
            } finally {
                typedArray.recycle();
            }
        }

        // 如果XML中没有指定，使用自动分配逻辑
        return getNextInstanceId();
    }

    /**
     * 获取下一个实例ID（自动分配主副PIP）
     */
    private static synchronized String getNextInstanceId() {
        instanceCounter++;
        switch (instanceCounter) {
            case 1:
                return INSTANCE_MAIN;
            case 2:
                return INSTANCE_SECONDARY;
            default:
                return "pip_" + instanceCounter;
        }
    }

    /**
     * 获取实例唯一标识
     */
    public String getInstanceId() {
        return instanceId;
    }

    /**
     * 检查应用是否正在运行
     */
    public boolean isAppRunning() {
        return isAppRunning;
    }

    /**
     * 处理屏幕方向变化
     */
    public void onOrientationChanged(boolean isLandscape) {
        // 可以在这里调整画中画的显示参数
        // 比如根据屏幕方向调整分辨率
        KLog.d(TAG + " Orientation changed to: " + (isLandscape ? "landscape" : "portrait"));
    }

    /**
     * 重置PIPView：停止应用、销毁virtualdisplay、重启virtualdisplay和应用
     */
    private void resetPIPView() {
        KLog.d(TAG + " [" + instanceId + "] Resetting PIP view");

        // 1. 停止当前应用
        stopApp();

        // 2. 销毁virtualdisplay
        if (mVirtualDisplay != null) {
            KLog.d(TAG + " [" + instanceId + "] Releasing virtual display");
            mVirtualDisplay.release();
            mVirtualDisplay = null;
            displayId = -1;
        }

        // 3. 重启virtualdisplay
        if (mSurfaceView != null && mSurfaceView.getSurfaceTexture() != null) {
            KLog.d(TAG + " [" + instanceId + "] Recreating virtual display");
            createVirtualDisplayWithSurfaceTexture(mSurfaceView.getSurfaceTexture());
        }

        // 4. 重启应用（如果有保存的应用配置）
        if (configManager != null && configManager.hasSavedAppConfig()) {
            String savedPackageName = configManager.getSelectedAppPackage();
            if (savedPackageName != null && !savedPackageName.isEmpty()) {
                KLog.d(TAG + " [" + instanceId + "] Restarting saved app: " + savedPackageName);
                // 延迟启动，确保virtualdisplay已准备好
                postDelayed(() -> {
                    if (displayId != -1) {
                        startAppOnDisplay(savedPackageName, displayId);
                    }
                }, 500); // 500ms延迟
            }
        }

        // 显示重置完成提示
        showError("PIP视图已重置");
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        stopApp();

        // 清理触摸事件处理器
        if (touchEventHandler != null) {
            touchEventHandler.cleanup();
            touchEventHandler = null;
        }

        // Texture模式下不需要停止MediaProjection
        KLog.d(TAG + " Detached from window (Texture mode)");
    }

}