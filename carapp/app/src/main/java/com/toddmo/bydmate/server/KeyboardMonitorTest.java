package com.toddmo.bydmate.server;

import android.util.Log;

import com.toddmo.bydmate.client.utils.KLog;

/**
 * KeyboardMonitor 测试类
 * 演示如何使用 KeyboardMonitor
 */
public class KeyboardMonitorTest {

    private static final String TAG = "KeyboardMonitorTest";

    public static void test() {
        KLog.i(TAG, "Starting KeyboardMonitor test");

        // 设置监听器
        KeyboardMonitor.setKeyboardListener(new KeyboardMonitor.KeyboardListener() {
            @Override
            public void onKeyEvent(int keyCode, int action) {
                KLog.i(TAG, "Key event: keyCode=" + keyCode + ", action=" + action);

                // 示例：监听回车键按下
                if (keyCode == 28 && action == 1) { // KEY_ENTER, ACTION_DOWN
                    KLog.i(TAG, "Enter key pressed!");
                }

                // 示例：监听 ESC 键释放
                if (keyCode == 1 && action == 0) { // KEY_ESC, ACTION_UP
                    KLog.i(TAG, "ESC key released!");
                }
            }
        });

        // 添加键值过滤器（可选）
        KeyboardMonitor.addKeyFilter(28); // 只监听回车键
        KeyboardMonitor.addKeyFilter(1);  // 只监听 ESC 键

        // 启动监听
        KeyboardMonitor.startMonitoring();

        KLog.i(TAG, "Keyboard monitoring started. Press keys to test...");

        // 在实际应用中，你可能需要一个循环来保持程序运行
        // 这里只是演示
    }

    public static void stopTest() {
        KLog.i(TAG, "Stopping KeyboardMonitor test");

        // 停止监听
        KeyboardMonitor.stopMonitoring();

        // 清除过滤器
        KeyboardMonitor.clearKeyFilters();

        KLog.i(TAG, "Keyboard monitoring stopped");
    }

    /**
     * 测试所有键盘事件（不使用过滤器）
     */
    public static void testAllKeys() {
        KLog.i(TAG, "Starting KeyboardMonitor test for all keys");

        // 设置监听器
        KeyboardMonitor.setKeyboardListener(new KeyboardMonitor.KeyboardListener() {
            @Override
            public void onKeyEvent(int keyCode, int action) {
                String actionStr = action == 1 ? "DOWN" : "UP";
                KLog.i(TAG, "Key event: keyCode=" + keyCode + ", action=" + actionStr);
            }
        });

        // 不添加过滤器，监听所有键盘事件
        KeyboardMonitor.clearKeyFilters();

        // 启动监听
        KeyboardMonitor.startMonitoring();

        KLog.i(TAG, "Keyboard monitoring started for all keys. Press any key to test...");
    }

    /**
     * 测试音量键监听
     */
    public static void testVolumeKeys() {
        KLog.i(TAG, "Starting KeyboardMonitor test for volume keys");

        // 设置监听器
        KeyboardMonitor.setKeyboardListener(new KeyboardMonitor.KeyboardListener() {
            @Override
            public void onKeyEvent(int keyCode, int action) {
                String actionStr = action == 1 ? "DOWN" : "UP";

                if (keyCode == 115 && action == 1) { // KEY_VOLUMEUP, ACTION_DOWN
                    KLog.i(TAG, "🔊 Volume UP key pressed!");
                } else if (keyCode == 114 && action == 1) { // KEY_VOLUMEDOWN, ACTION_DOWN
                    KLog.i(TAG, "🔉 Volume DOWN key pressed!");
                } else if (keyCode == 115 && action == 0) { // KEY_VOLUMEUP, ACTION_UP
                    KLog.i(TAG, "🔊 Volume UP key released");
                } else if (keyCode == 114 && action == 0) { // KEY_VOLUMEDOWN, ACTION_UP
                    KLog.i(TAG, "🔉 Volume DOWN key released");
                } else {
                    KLog.d(TAG, "Other key event: keyCode=" + keyCode + ", action=" + actionStr);
                }
            }
        });

        // 只监听音量键
        KeyboardMonitor.clearKeyFilters();
        KeyboardMonitor.addKeyFilter(115); // KEY_VOLUMEUP
        KeyboardMonitor.addKeyFilter(114); // KEY_VOLUMEDOWN

        // 启动监听
        KeyboardMonitor.startMonitoring();

        KLog.i(TAG, "Keyboard monitoring started for volume keys. Press volume +/- keys to test...");
    }

    /**
     * 测试音量键和常用键的组合监听
     */
    public static void testVolumeAndCommonKeys() {
        KLog.i(TAG, "Starting KeyboardMonitor test for volume and common keys");

        // 设置监听器
        KeyboardMonitor.setKeyboardListener(new KeyboardMonitor.KeyboardListener() {
            @Override
            public void onKeyEvent(int keyCode, int action) {
                String actionStr = action == 1 ? "DOWN" : "UP";

                switch (keyCode) {
                    case 115: // KEY_VOLUMEUP
                        KLog.i(TAG, "🔊 Volume UP " + actionStr);
                        break;
                    case 114: // KEY_VOLUMEDOWN
                        KLog.i(TAG, "🔉 Volume DOWN " + actionStr);
                        break;
                    case 28: // KEY_ENTER
                        KLog.i(TAG, "⏎ Enter " + actionStr);
                        break;
                    case 1: // KEY_ESC
                        KLog.i(TAG, "⎋ ESC " + actionStr);
                        break;
                    default:
                        KLog.d(TAG, "Key event: keyCode=" + keyCode + ", action=" + actionStr);
                        break;
                }
            }
        });

        // 监听音量键和常用键
        KeyboardMonitor.clearKeyFilters();
        KeyboardMonitor.addKeyFilter(115); // KEY_VOLUMEUP
        KeyboardMonitor.addKeyFilter(114); // KEY_VOLUMEDOWN
        KeyboardMonitor.addKeyFilter(28);  // KEY_ENTER
        KeyboardMonitor.addKeyFilter(1);   // KEY_ESC

        // 启动监听
        KeyboardMonitor.startMonitoring();

        KLog.i(TAG, "Keyboard monitoring started for volume and common keys. Press volume +/- or Enter/ESC keys to test...");
    }
}