package com.toddmo.bydmate.server;

/**
 * 继续编写@/app/src/main/java/com/toddmo/bydmate/server/KeyboardMonitor.java，
 * 参考AOSP toolbox内的getevent实现， 实现如下功能：
 * 1. 监听/dev/input下的设备接入和移除状态
 * 2. 当有键盘插入时，打开其设备句柄，并注册对该FD的非阻塞读写监听（如epoll、select等）
 * 3. 定义一个HashSet，存放需要监听的键盘事件键值，其他不在Set之内的事件丢弃
 * 4. 提供键盘注册特定键值的Listener回调
 */

public class KeyboardMonitor {

}