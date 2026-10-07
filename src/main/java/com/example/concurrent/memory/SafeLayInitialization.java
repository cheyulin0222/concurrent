package com.example.concurrent.memory;

// 同步延遲初始化（Synchronized Lazy Initialization）


// 適用情境：若此方法呼叫頻率不高、執行緒競爭少，鎖的開銷極低，這是最直觀且足夠安全的解法。
// 缺點：在高並發存取情境下，每次讀取都需獲取監視器鎖，容易產生效能瓶頸。
public class SafeLayInitialization {
    private static Resource resource;

    // 直接在 getInstance() 方法加上 synchronized 關鍵字
    public synchronized static Resource getInstance() {
        if (resource == null)
            resource = new Resource();
        return resource;
    }
}
