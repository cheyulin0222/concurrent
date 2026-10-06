package com.example.concurrent.explicitlocks;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public class ReentrantLockAvoidDeadLockRetriesStudy {

    private static final int MAX_RETRIES = 5;
    private static final long TIME_OUT = 200;
    private static final long BASE_DELAY_MS = 100; // 基礎延遲 100ms
    private static final long MAX_DELAY_MS = 3000; // 最大上限 2 秒，避免無限膨脹

    public boolean transferMoney(Account from, Account to, int amount) {

        int attempts = 0;

        while (attempts < MAX_RETRIES) {
            attempts++;
            try {
                if (from.lock.tryLock(TIME_OUT, TimeUnit.MILLISECONDS)) {
                    try {
                        if (to.lock.tryLock(TIME_OUT, TimeUnit.MILLISECONDS)) {
                            try {
                                if (from.getBalance().compareTo(amount) < 0) throw new RuntimeException("餘額不足");
                                else {
                                    from.checkout(amount);
                                    to.checkIn(amount);
                                    return true;
                                }
                            } finally {
                                to.lock.unlock();
                            }
                        }
                    } finally {
                        from.lock.unlock();
                    }
                }

                // 指數退避上限計算：100, 200, 400, 800...
                // 指數退避實作 （Exponential Backoff)
                // 分散式/網路層「強烈需要」，但在純記憶體內的鎖「通常不用」
                // 如果遠端的資料庫已經快被塞爆了，如果你失敗了還每隔 100ms 瘋狂敲門
                // 只會引發重試風暴（Retry Storm），把快死掉的伺服器徹底打趴。
                // 1L << 1  2 的 2 次方
                // 1L << 2  2 的 2 次方
                long currentUpper = Math.min(BASE_DELAY_MS * (1L << attempts), MAX_DELAY_MS);
                TimeUnit.MILLISECONDS.sleep(ThreadLocalRandom.current().nextLong(BASE_DELAY_MS, currentUpper));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }

        // 嘗試失敗
        return false;
    }

    static class Account {
        private Lock lock;
        private int balance;

        public Account(int amount) {
            this.lock = new ReentrantLock();
            this.balance = amount;
        }

        public Integer getBalance() {
            return balance;
        }

        public void checkIn(int amount) {
            this.balance += amount;
        }

        public void checkout(int amount) {
            this.balance -= amount;
        }

    }
}
