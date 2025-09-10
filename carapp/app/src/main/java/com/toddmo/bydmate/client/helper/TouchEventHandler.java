package com.toddmo.bydmate.client.helper;

import android.os.IBinder;
import android.os.RemoteException;
import android.view.MotionEvent;
import android.view.View;

import com.toddmo.bydmate.aidl.IBydMateServer;
import com.toddmo.bydmate.client.utils.DataHolder;
import com.toddmo.bydmate.client.utils.KLog;
import com.toddmo.bydmate.server.BinderParcelable;

import java.util.HashMap;
import java.util.Map;

/**
 * ADB触摸事件处理器
 * 负责将Android MotionEvent转换为ADB input命令并发送到虚拟设备
 */
public class TouchEventHandler {

    private static final String TAG = "TouchEventHandler";

    // ADB命令常量
    private static final String INPUT_COMMAND = "input";
    private static final String TAP_SUBCOMMAND = "tap";
    private static final String SWIPE_SUBCOMMAND = "swipe";
    private static final String MOTIONEVENT_SUBCOMMAND = "motionevent";

    // 事件状态跟踪
    private final Map<Integer, TouchPoint> activeTouches = new HashMap<>();
    private int displayId = -1;

    // 坐标转换参数
    private int textureViewWidth = 0;
    private int textureViewHeight = 0;
    private int textureViewLeft = 0;
    private int textureViewTop = 0;
    private int virtualScreenWidth = 0;
    private int virtualScreenHeight = 0;

    // 连接状态跟踪
    private boolean isAdbConnected = false;
    private int connectionRetryCount = 0;
    private static final int MAX_RETRY_COUNT = 3;
    private static final long RETRY_DELAY_MS = 1000;

    /**
     * 触摸点信息
     */
    private static class TouchPoint {
        float startX, startY;
        float currentX, currentY;
        long startTime;
        boolean isMoving;

        TouchPoint(float x, float y) {
            this.startX = this.currentX = x;
            this.startY = this.currentY = y;
            this.startTime = System.currentTimeMillis();
            this.isMoving = false;
        }

        void updatePosition(float x, float y) {
            this.currentX = x;
            this.currentY = y;
            this.isMoving = true;
        }

        boolean hasMoved() {
            return isMoving;
        }

        float getDistance() {
            float dx = currentX - startX;
            float dy = currentY - startY;
            return (float) Math.sqrt(dx * dx + dy * dy);
        }
    }

    /**
     * 设置显示ID
     */
    public void setDisplayId(int displayId) {
        this.displayId = displayId;
        KLog.d(TAG + " Display ID set to: " + displayId);
    }

    /**
     * 设置坐标转换参数
     */
    public void setCoordinateTransform(int textureViewLeft, int textureViewTop,
                                     int textureViewWidth, int textureViewHeight,
                                     int virtualScreenWidth, int virtualScreenHeight) {
        this.textureViewLeft = textureViewLeft;
        this.textureViewTop = textureViewTop;
        this.textureViewWidth = textureViewWidth;
        this.textureViewHeight = textureViewHeight;
        this.virtualScreenWidth = virtualScreenWidth;
        this.virtualScreenHeight = virtualScreenHeight;

        KLog.d(TAG + " Coordinate transform set: " +
               "Position(" + textureViewLeft + "," + textureViewTop + ") " +
               "Size(" + textureViewWidth + "x" + textureViewHeight + ") -> " +
               virtualScreenWidth + "x" + virtualScreenHeight);
    }

    /**
     * 处理触摸事件
     */
    public boolean handleTouchEvent(View view, MotionEvent event) {
        if (displayId == -1) {
            KLog.w(TAG + " Display ID not set, cannot handle touch events");
            return false;
        }

        if (textureViewWidth == 0 || textureViewHeight == 0 ||
            virtualScreenWidth == 0 || virtualScreenHeight == 0) {
            KLog.w(TAG + " Coordinate transform not set, cannot handle touch events");
            return false;
        }

        int action = event.getActionMasked();
        int pointerIndex = event.getActionIndex();
        int pointerId = event.getPointerId(pointerIndex);

//        switch (action) {
//            case MotionEvent.ACTION_DOWN:
//            case MotionEvent.ACTION_POINTER_DOWN:
//                handlePointerDown(pointerId, event.getX(pointerIndex), event.getY(pointerIndex));
//                break;
//
//            case MotionEvent.ACTION_MOVE:
//                handlePointerMove(event);
//                break;
//
//            case MotionEvent.ACTION_UP:
//            case MotionEvent.ACTION_POINTER_UP:
//                handlePointerUp(pointerId, event.getX(pointerIndex), event.getY(pointerIndex));
//                break;
//
//            case MotionEvent.ACTION_CANCEL:
//                handleCancel();
//                break;
//        }

        BinderParcelable binder = (BinderParcelable)DataHolder.getObject("service");
        if (binder == null) {
            KLog.e("no valid service");
            return true;
        }
        IBydMateServer server = binder.server;
        try {
            server.injectInputEvent(event, displayId);
        } catch (RemoteException e) {
            KLog.e("failed to injectInputEvent displayId " + displayId);
        }

        return true;
    }

    /**
     * 处理触摸按下
     */
    private void handlePointerDown(int pointerId, float x, float y) {
        TouchPoint touchPoint = new TouchPoint(x, y);
        activeTouches.put(pointerId, touchPoint);

        KLog.d(TAG + " Pointer " + pointerId + " down at (" + x + ", " + y + ")");

        // 对于单点触摸，立即发送按下事件
        if (activeTouches.size() == 1) {
            sendTouchDown(transformX(x), transformY(y));
        }
    }

    /**
     * 处理触摸移动
     */
    private void handlePointerMove(MotionEvent event) {
        for (int i = 0; i < event.getPointerCount(); i++) {
            int pointerId = event.getPointerId(i);
            TouchPoint touchPoint = activeTouches.get(pointerId);
            if (touchPoint != null) {
                float x = event.getX(i);
                float y = event.getY(i);
                touchPoint.updatePosition(x, y);
            }
        }

        // 发送移动事件
        if (activeTouches.size() == 1) {
            TouchPoint touchPoint = activeTouches.values().iterator().next();
            sendTouchMove(transformX(touchPoint.currentX), transformY(touchPoint.currentY));
        }
    }

    /**
     * 处理触摸抬起
     */
    private void handlePointerUp(int pointerId, float x, float y) {
        TouchPoint touchPoint = activeTouches.get(pointerId);
        if (touchPoint != null) {
            touchPoint.updatePosition(x, y);

            KLog.d(TAG + " Pointer " + pointerId + " up at (" + x + ", " + y + ")");

            if (activeTouches.size() == 1) {
                // 单点触摸结束
                if (touchPoint.hasMoved() && touchPoint.getDistance() > 10) {
                    // 移动距离足够大，认为是拖拽
                    sendTouchSwipe(transformX(touchPoint.startX), transformY(touchPoint.startY),
                                  transformX(touchPoint.currentX), transformY(touchPoint.currentY));
                } else {
                    // 移动距离小，认为是点击
                    sendTouchTap(transformX(touchPoint.currentX), transformY(touchPoint.currentY));
                }
                sendTouchUp(transformX(touchPoint.currentX), transformY(touchPoint.currentY));
            }

            activeTouches.remove(pointerId);
        }
    }

    /**
     * 处理取消事件
     */
    private void handleCancel() {
        KLog.d(TAG + " Touch event cancelled");
        activeTouches.clear();
        sendTouchCancel();
    }

    /**
     * 坐标转换：TextureView坐标 -> 虚拟屏幕坐标
     * 需要考虑TextureView在父容器中的位置偏移
     */
    private int transformX(float rawX) {
        if (textureViewWidth == 0) return 0;
        // 减去TextureView的left偏移，得到相对于TextureView本身的坐标
        float relativeX = rawX - textureViewLeft;
        // 确保坐标在有效范围内
        relativeX = Math.max(0, Math.min(textureViewWidth, relativeX));
        // 按比例缩放到虚拟屏幕
        float ratio = (float) virtualScreenWidth / textureViewWidth;
        int virtualX = Math.round(relativeX * ratio);
        return Math.max(0, Math.min(virtualScreenWidth - 1, virtualX));
    }

    private int transformY(float rawY) {
        if (textureViewHeight == 0) return 0;
        // 减去TextureView的top偏移，得到相对于TextureView本身的坐标
        float relativeY = rawY - textureViewTop;
        // 确保坐标在有效范围内
        relativeY = Math.max(0, Math.min(textureViewHeight, relativeY));
        // 按比例缩放到虚拟屏幕
        float ratio = (float) virtualScreenHeight / textureViewHeight;
        int virtualY = Math.round(relativeY * ratio);
        return Math.max(0, Math.min(virtualScreenHeight - 1, virtualY));
    }

    /**
     * 发送ADB触摸命令
     */
    private void sendAdbCommand(String command) {
        try {
            // 检查ADB连接状态
            if (!isAdbConnected) {
                checkAdbConnection();
            }

            // 添加display参数
            String fullCommand = command;

            KLog.d(TAG + " Sending ADB command: " + fullCommand);

            // 使用AdbHelper发送命令
            AdbHelper.ADBCommandThread thread = AdbHelper.getInstance().executeAsync(fullCommand);

            if (thread == null) {
                // 连接失败，尝试重连
                // handleConnectionFailure();
            } else {
                // 命令发送成功，重置重试计数
                connectionRetryCount = 0;
                isAdbConnected = true;
            }

        } catch (Exception e) {
            KLog.e(TAG + " Failed to send ADB command: " + e.getMessage());
            // handleConnectionFailure();
        }
    }

    /**
     * 检查ADB连接状态
     */
    private void checkAdbConnection() {
        try {
            isAdbConnected = AdbHelper.getInstance().checkConnectable("tcp", "127.0.0.1", 5555);
            if (isAdbConnected) {
                KLog.d(TAG + " ADB connection is active");
            } else {
                KLog.w(TAG + " ADB connection is not active");
            }
        } catch (Exception e) {
            KLog.e(TAG + " Error checking ADB connection: " + e.getMessage());
            isAdbConnected = false;
        }
    }

    /**
     * 处理连接失败
     */
    private void handleConnectionFailure() {
        isAdbConnected = false;
        connectionRetryCount++;

        if (connectionRetryCount <= MAX_RETRY_COUNT) {
            KLog.w(TAG + " ADB connection failed, retrying... (attempt " + connectionRetryCount + "/" + MAX_RETRY_COUNT + ")");

            // 延迟重试
            new Thread(() -> {
                try {
                    Thread.sleep(RETRY_DELAY_MS);
                    checkAdbConnection();
                    if (isAdbConnected) {
                        KLog.i(TAG + " ADB connection restored");
                        connectionRetryCount = 0;
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();
        } else {
            KLog.e(TAG + " ADB connection failed after " + MAX_RETRY_COUNT + " attempts");
        }
    }

    private void sendTouchDown(int x, int y) {
        String inputCommand = INPUT_COMMAND;
        if (displayId != -1) {
            inputCommand = inputCommand + " -d " + displayId;
        }
        String command = inputCommand + " " + MOTIONEVENT_SUBCOMMAND + " DOWN " + x + " " + y;
//        sendAdbCommand(command);
    }

    private void sendTouchMove(int x, int y) {
        String inputCommand = INPUT_COMMAND;
        if (displayId != -1) {
            inputCommand = inputCommand + " -d " + displayId;
        }
        String command = inputCommand + " " + MOTIONEVENT_SUBCOMMAND + " MOVE " + x + " " + y;
//        sendAdbCommand(command);
    }

    private void sendTouchUp(int x, int y) {
        String inputCommand = INPUT_COMMAND;
        if (displayId != -1) {
            inputCommand = inputCommand + " -d " + displayId;
        }
        String command = inputCommand + " " + MOTIONEVENT_SUBCOMMAND + " UP " + x + " " + y;
//        sendAdbCommand(command);
    }

    private void sendTouchTap(int x, int y) {
        String inputCommand = INPUT_COMMAND;
        if (displayId != -1) {
            inputCommand = inputCommand + " -d " + displayId;
        }
        String command = inputCommand + " " + TAP_SUBCOMMAND + " " + x + " " + y;
        sendAdbCommand(command);
    }

    private void sendTouchSwipe(int startX, int startY, int endX, int endY) {
        String inputCommand = INPUT_COMMAND;
        if (displayId != -1) {
            inputCommand = inputCommand + " -d " + displayId;
        }
        String command = inputCommand + " " + SWIPE_SUBCOMMAND + " " + startX + " " + startY + " " + endX + " " + endY;
//        sendAdbCommand(command);
    }

    private void sendTouchCancel() {
        String inputCommand = INPUT_COMMAND;
        if (displayId != -1) {
            inputCommand = inputCommand + " -d " + displayId;
        }
        String command = inputCommand + " " + MOTIONEVENT_SUBCOMMAND + " CANCEL";
//        sendAdbCommand(command);
    }

    /**
     * 获取活动触摸点数量
     */
    public int getActiveTouchCount() {
        return activeTouches.size();
    }

    /**
     * 清理资源
     */
    public void cleanup() {
        activeTouches.clear();
        KLog.d(TAG + " TouchEventHandler cleaned up");
    }
}