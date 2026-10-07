package com.example.concurrent.nonblocking;

public class CasCounter {
    private SimulateCAS value;

    public int getValue() {
        return value.get();
    }

    public int increment() {
        int v;
        do {
            v = value.get(); // 讀取當前舊值
        }
        // 2. 嘗試將 v 原子替換為 v + 1
        //    如果 compareAndSwap 返回值 != v，代表這段期間有其他執行緒修改了值（CAS 失敗），繼續重試
        while (v != value.compareAndSwap(v, v + 1));

        return v + 1; // 成功更新後返回新值
    }
}
