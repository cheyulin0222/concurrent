package com.example.concurrent.gui;

// 實作類別：完全不支援多執行緒，只能由同一個執行緒呼叫
public class UnsafeNativeDevice implements LegacyNativeDevice {
    @Override
    public String sendCommand(String cmd) {
        // 模擬呼叫 JNI / C 語言函式庫
        System.out.println("Native 呼叫中: " + cmd + " [執行緒: " + Thread.currentThread().getName() + "]");
        return "OK: " + cmd;
    }
}
