package com.example.concurrent.memory;

// 核心優勢（結合延遲載入與 JVM 執行期保證）：
//
// 按需載入（真正的 Lazy）：
// 外層 ResourceFactory 被載入時，JVM 並不會預先載入內部類別 ResourceHolder；只有在第一次呼叫 getResource() 時，才會觸發 ResourceHolder 的載入與初始化。
//
// 零同步開銷：底層依賴 JVM 靜態初始化的內部鎖機制保證執行緒安全與可見性，常態讀取路徑完全不需要任何 synchronized。
public class ResourceFactory {
    private static class ResourceHolder {
        public static Resource resource = new Resource();
    }

    public static Resource getResource() {
        return ResourceHolder.resource;
    }
}
