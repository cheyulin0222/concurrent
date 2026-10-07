package com.example.concurrent.self;

import java.util.concurrent.locks.AbstractQueuedSynchronizer;

// 太空總署要發射火箭，
// 總共有 3 項檢查必須全部過關（new CountDownLatch(3)）：
// 燃料加注完成  打勾（countDown()，剩 2）
// 航電系統測試完成 打勾（countDown()，剩 1）
// 天氣合格 打勾（countDown()，倒數歸零！）
// 火箭（呼叫 await() 的執行緒）在檢查完成前絕對不准發射。只要計數器一歸零，火箭立刻點火升空。重點：只能扣減、不能加回去，這是一次性的（One-Shot）。

// 在還沒扣到 0 之前，一律回傳 false，
// AQS 誰也不叫；只有扣到 0 的那一瞬間回傳 true，觸發全體喚醒。
public class CountDownLatchDemo {
    private final Sync sync = new Sync();

    private class Sync extends AbstractQueuedSynchronizer {
        protected boolean tryReleaseShared(int releases) {
            for (;;) {
                int c = getState();
                if (c == 0) return false;
                int nextc = c - 1;
                if (compareAndSetState(c, nextc))
                    return nextc == 0;
            }
        }
        protected int tryAcquireShared(int acquires) {
            return (getState() == 0) ? 1 : -1;
        }
    }


}
