package com.example.concurrent.self;

// ReentrantLock 走的是獨占模式（Exclusive Mode），
// 同一時間只准許單一執行緒持有。

// 釋放鎖（tryRelease）：
// 持鎖者呼叫 unlock() 時，執行 state - 1。
// 只有扣減到 state == 0 時，才會清空 owner 並回傳 true，
// 通知 AQS 喚醒佇列中的後繼節點。

// Condition 支援：
// 直接呼叫 AQS 內建的內部類別 ConditionObject，
// 即可建立獨立的等待隊列（Wait Set）。
public class ReentrantLockDemo {

    private final Sync sync = new Sync();
    private class Sync extends AbstractQueuedSynchronizer {
        protected boolean tryAcquire(int ignored) {
            Thread current = Thread.currentThread();
            int c = getState();

            // 1. 鎖目前處於空閒狀態
            if (c == 0) {
                if (compareAndSetState(0, 1)) {
                    // 標記持鎖者為自己
                    owner = current;
                    return true;
                }
                // 2. 鎖已被佔用，但持鎖者「就是當前執行緒」（可重入判斷）
            } else if (current == owner) {
                // 重入深度 + 1（此處持鎖，無競爭，無需 CAS）
                setState(c + 1);
                return true;
            }
            return false;
        }
    }

}
