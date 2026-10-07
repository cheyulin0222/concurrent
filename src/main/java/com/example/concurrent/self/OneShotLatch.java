package com.example.concurrent.self;

import java.util.concurrent.locks.AbstractQueuedSynchronizer;

// 1. OneShotLatch 只需要對外暴露 await() 與 signal()
// 若直接繼承 AQS，會把 AQS 龐雜的公用方法暴露給外部使用者。
public class OneShotLatch {
    private final Sync sync = new Sync();

    // （對應共享釋放 Release）：
    // 將門閂狀態設定為開啟（state = 1），
    // 喚醒所有正在阻塞等待的執行緒。此門閂為「一次性（One-Shot）」，
    // 開啟後便維持開啟狀態。
    public void signal() {
        sync.releaseShared(0);
    }

    // await()（對應共享獲取 Acquire）：
    // 門閂若為關閉（state == 0），呼叫的執行緒會阻塞等待；
    // 一旦門閂被打開，後續抵達的執行緒可直接通過。
    public void await() throws InterruptedException {
        sync.acquireSharedInterruptibly(0);
    }

    private class Sync extends AbstractQueuedSynchronizer {
        // 回傳 「剩餘額度」 或 「成功狀態」
        protected int tryAcquireShared(int ignored) {
            // Succeed if latch is open (state == 1), else fail
            return (getState() == 1) ? 1 : -1;
        }

        protected boolean tryReleaseShared(int ignored) {
            setState(1); // Latch is now open
            return true; // Other threads may now be able to acquire
        }
    }
}
