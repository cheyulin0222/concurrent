package com.example.concurrent.memory;

// JVM 底層保證：
// 靜態初始區塊（Static Initializer）是在「類別載入後、被任何執行緒使用前」由 JVM 統一執行。
// JVM 在類別初始化期間會取得一把內部初始化鎖，因此在靜態初始化期間發生的所有記憶體寫入，自動對所有執行緒可見。

// 適用情境：初始化負擔不大，或該類別載入時必然會用到該物件，完全免去執行期的同步開銷。
// 注意事項：JVM 僅保證「剛建構完畢的狀態」安全發布；若該物件是可變的（Mutable），後續的讀寫修改仍需手動同步。
public class EagerInitialization {
    private static Resource resource = new Resource();

    public static Resource getResource() {
        return resource;
    }
}
