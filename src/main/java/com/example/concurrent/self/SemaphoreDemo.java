package com.example.concurrent.self;
// Semaphore：遊樂設施的「進場號碼牌」或「收費停車場」
// 停車場只有 3 個車位（new Semaphore(3)）。

// 前 3 輛車拿到車位（acquire()）進去停。
// 第 4 輛車必須在門口排隊等。

// Semaphore 走的是共享模式（Shared Mode），
// 多個執行緒可以同時持有許可

// 若 remaining < 0：回傳負數，AQS 將當前執行緒掛起。
// 若 remaining > 0：表示還有剩餘許可，
// AQS 會透過 setHeadAndPropagate 連續喚醒後續的排隊者一起拿取。
public class SemaphoreDemo {
    private final Sync sync = new Sync();

    private class Sync extends AbstractQueuedSynchronizer {
        protected int tryAcquireShared(int acquires) {
            while (true) {
                int available = getState();
                int remaining = available - acquires; // 算出扣減後的剩餘量


                // 剩餘量小於 0（名額不足，獲取失敗，直接回傳負數走進佇列排隊）
                // 或者 CAS 扣減成功，回傳剩餘額度 (>= 0 代表成功)
                if (remaining < 0 || compareAndSetState(available, remaining))
                    return remaining;
            }
        }

        // 歸還許可證（共享釋放）
        protected boolean tryReleaseShared(int releases) {
            while (true) {
                int p = getState();

                if (compareAndSetState(p, p + releases))
                    return true; // 回傳 true 通知 AQS 喚醒排隊等待許可證的節點
            }
        }
    }
}
