package com.example.concurrent.gui;

// 假設這是一個非執行緒安全的 Native 服務介面
public interface LegacyNativeDevice {
    String sendCommand(String cmd);
}
