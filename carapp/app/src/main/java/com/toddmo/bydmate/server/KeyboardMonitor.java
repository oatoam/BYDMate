package com.toddmo.bydmate.server;

import android.util.Log;

import com.toddmo.bydmate.client.utils.KLog;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 键盘监听器
 * 使用 getevent 命令监听键盘事件，实现如下功能：
 * 1. 监听/dev/input下的设备接入和移除状态
 * 2. 当有键盘插入时，打开其设备句柄，并注册对该FD的非阻塞读写监听
 * 3. 定义一个HashSet，存放需要监听的键盘事件键值，其他不在Set之内的事件丢弃
 * 4. 提供键盘注册特定键值的Listener回调
 */
public class KeyboardMonitor {

    private static final String TAG = "KeyboardMonitor";

    // 键盘事件监听器接口
    public interface KeyboardListener {
        void onKeyEvent(int keyCode, int action);
    }

    private static KeyboardListener listener;
    private static final AtomicBoolean isMonitoring = new AtomicBoolean(false);
    private static final Set<Integer> keyFilters = new HashSet<>();
    private static ExecutorService executorService;
    private static Process geteventProcess;
    private static BufferedReader reader;

    /**
     * 启动键盘监听
     */
    public static void startMonitoring() {
        if (isMonitoring.get()) {
            KLog.w("Keyboard monitoring is already running");
            return;
        }

        try {
            startGeteventProcess();
            isMonitoring.set(true);
            KLog.i( "Keyboard monitoring started");
        } catch (Exception e) {
            KLog.e( "Failed to start keyboard monitoring" + e);
        }
    }

    /**
     * 停止键盘监听
     */
    public static void stopMonitoring() {
        if (!isMonitoring.get()) {
            KLog.w( "Keyboard monitoring is not running");
            return;
        }

        try {
            stopGeteventProcess();
            isMonitoring.set(false);
            KLog.i( "Keyboard monitoring stopped");
        } catch (Exception e) {
            KLog.e( "Failed to stop keyboard monitoring" + e);
        }
    }

    /**
     * 设置键盘事件监听器
     */
    public static void setKeyboardListener(KeyboardListener listener) {
        KeyboardMonitor.listener = listener;
    }

    /**
     * 添加键值过滤器
     * 如果添加了过滤器，只监听指定键值的事件
     */
    public static void addKeyFilter(int keyCode) {
        synchronized (keyFilters) {
            keyFilters.add(keyCode);
        }
        KLog.d( "Added key filter: " + keyCode);
    }

    /**
     * 移除键值过滤器
     */
    public static void removeKeyFilter(int keyCode) {
        synchronized (keyFilters) {
            keyFilters.remove(keyCode);
        }
        KLog.d( "Removed key filter: " + keyCode);
    }

    /**
     * 清除所有键值过滤器
     */
    public static void clearKeyFilters() {
        synchronized (keyFilters) {
            keyFilters.clear();
        }
        KLog.d( "Cleared all key filters");
    }

    /**
     * 检查是否正在监听
     */
    public static boolean isMonitoring() {
        return isMonitoring.get();
    }

    /**
     * 获取当前过滤的键值集合
     */
    public static Set<Integer> getKeyFilters() {
        synchronized (keyFilters) {
            return new HashSet<>(keyFilters);
        }
    }

    /**
     * 启动 getevent 进程
     */
    private static void startGeteventProcess() throws IOException {
        // 创建线程池
        executorService = Executors.newSingleThreadExecutor();

        // 启动 getevent 进程监听所有输入设备
        ProcessBuilder processBuilder = new ProcessBuilder("getevent", "-l");
        processBuilder.redirectErrorStream(true);
        geteventProcess = processBuilder.start();
        KLog.d("processBuilder");

        // 获取输出流
        reader = new BufferedReader(new InputStreamReader(geteventProcess.getInputStream()));

        // 启动监听线程
        executorService.submit(() -> {
            KLog.d("reader executor begin");
            try {
                String line;
                while ((line = reader.readLine()) != null && isMonitoring.get()) {
                    processGeteventLine(line);
                }
            } catch (IOException e) {
                if (isMonitoring.get()) {
                    KLog.e( "Error reading getevent output" + e);
                }
            } finally {
                KLog.d( "Getevent reader thread finished");
            }
        });

        KLog.d( "Getevent process started");
    }

    /**
     * 停止 getevent 进程
     */
    private static void stopGeteventProcess() {
        try {
            if (reader != null) {
                reader.close();
                reader = null;
            }

            if (geteventProcess != null) {
                geteventProcess.destroy();
                try {
                    // 等待进程结束，最多等待 5 秒
                    if (!geteventProcess.waitFor(5, java.util.concurrent.TimeUnit.SECONDS)) {
                        geteventProcess.destroyForcibly();
                    }
                } catch (InterruptedException e) {
                    geteventProcess.destroyForcibly();
                    Thread.currentThread().interrupt();
                }
                geteventProcess = null;
            }

            if (executorService != null) {
                executorService.shutdown();
                try {
                    if (!executorService.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS)) {
                        executorService.shutdownNow();
                    }
                } catch (InterruptedException e) {
                    executorService.shutdownNow();
                    Thread.currentThread().interrupt();
                }
                executorService = null;
            }

            KLog.d( "Getevent process stopped");
        } catch (Exception e) {
            KLog.e( "Error stopping getevent process" + e);
        }
    }

    /**
     * 处理 getevent 输出行
     * getevent -l 输出格式示例：
     * /dev/input/event0: EV_KEY KEY_ENTER DOWN
     * /dev/input/event0: EV_KEY KEY_ENTER UP
     */
    private static void processGeteventLine(String line) {
//        KLog.d("processGeteventLine: " + line);
        try {
            // 解析 getevent 输出
            if (!line.contains("EV_KEY")) {
                return; // 只处理键盘事件
            }
//            KLog.d("EV_KEY: " + line);

            String[] parts = line.split("\\s+");
//            KLog.d("EV_KEY: parts " + parts);
//            for (String part : parts) {
//                KLog.d("EV_KEY: part " + parts);
//            }
            if (parts.length < 4) {
                return;
            }

            // 提取键码和动作
            String keyName = parts[2]; // KEY_ENTER
            String action = parts[3];  // DOWN 或 UP

            // 转换键码
            int keyCode = parseKeyCode(keyName);
            if (keyCode == -1) {
                return; // 未知键码
            }

            // 转换动作
            int actionCode = parseAction(action);
            if (actionCode == -1) {
                return; // 未知动作
            }

            // 检查过滤器
            synchronized (keyFilters) {
                if (!keyFilters.isEmpty() && !keyFilters.contains(keyCode)) {
                    return; // 不符合过滤条件
                }
            }

            // 回调监听器
            if (listener != null) {
                try {
                    listener.onKeyEvent(keyCode, actionCode);
                } catch (Exception e) {
                    KLog.e( "Error in keyboard listener" + e);
                }
            }

        } catch (Exception e) {
            KLog.e( "Error processing getevent line: " + line + e);
        }
    }

    /**
     * 解析键码名称为数字
     */
    private static int parseKeyCode(String keyName) {
        switch (keyName) {
            case "KEY_ENTER": return 28;
            case "KEY_ESC": return 1;
            case "KEY_BACKSPACE": return 14;
            case "KEY_TAB": return 15;
            case "KEY_SPACE": return 57;
            case "KEY_LEFTSHIFT": return 42;
            case "KEY_RIGHTSHIFT": return 54;
            case "KEY_LEFTCTRL": return 29;
            case "KEY_RIGHTCTRL": return 97;
            case "KEY_LEFTALT": return 56;
            case "KEY_RIGHTALT": return 100;
            case "KEY_LEFTMETA": return 125;
            case "KEY_RIGHTMETA": return 126;
            case "KEY_CAPSLOCK": return 58;
            case "KEY_NUMLOCK": return 69;
            case "KEY_SCROLLLOCK": return 70;
            case "KEY_F1": return 59;
            case "KEY_F2": return 60;
            case "KEY_F3": return 61;
            case "KEY_F4": return 62;
            case "KEY_F5": return 63;
            case "KEY_F6": return 64;
            case "KEY_F7": return 65;
            case "KEY_F8": return 66;
            case "KEY_F9": return 67;
            case "KEY_F10": return 68;
            case "KEY_F11": return 87;
            case "KEY_F12": return 88;
            case "KEY_VOLUMEUP": return 115;
            case "KEY_VOLUMEDOWN": return 114;
            // 添加更多键码映射...
            default:
                // 尝试解析数字键码
                if (keyName.startsWith("KEY_")) {
                    try {
                        return Integer.parseInt(keyName.substring(4));
                    } catch (NumberFormatException e) {
                        KLog.w( "Unknown key: " + keyName);
                        return -1;
                    }
                }
                return -1;
        }
    }

    /**
     * 解析动作字符串
     */
    private static int parseAction(String action) {
        switch (action.toUpperCase()) {
            case "DOWN": return 1; // ACTION_DOWN
            case "UP": return 0;   // ACTION_UP
            default:
                KLog.w( "Unknown action: " + action);
                return -1;
        }
    }
}