package com.example.concurrent.explicitlocks;

import lombok.Getter;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;


public class ReentrantLockAvoidDeadLockStudy {

    private static final long TIME_OUT = 3;

    public boolean transfer(Account from, Account to, int amount) {

        long stopTime = System.nanoTime() + TimeUnit.SECONDS.toNanos(TIME_OUT);

        while (System.nanoTime() < stopTime) {
            try {
                if (from.lock.tryLock()) {
                    try {
                        if (to.lock.tryLock()) {
                            try {
                                if (from.getBalance() < amount) throw new RuntimeException("餘額不足");
                                else {
                                    from.checkout(amount);
                                    from.checkIn(amount);
                                    return true; // 成功
                                }
                            } finally {
                                to.lock.unlock();
                            }
                        }
                    } finally {
                        from.lock.unlock();
                    }
                }
                // 1. 避免太早重試
                // 2. 避免活鎖，兩人同時醒來、又在同一個毫秒再次各自搶到 A 和 B，接著又因為搶不到對方的鎖而同時放手。
                // 隨機性（Jitter）的作用：透過隨機的時間差（例如一個睡 120ms、另一個睡 280ms），打亂彼此的步調，讓其中一方先醒來一口氣把兩把鎖拿齊並完成任務。
                // java.util.Random 產生隨機數時，必須更新內部共享的「種子變數（Seed）」。
                // 為了保證多執行緒安全，它底層使用了 AtomicLong 的 CAS（Compare-And-Swap）迴圈。
                // 災難發生在併發時：如果有 50 個執行緒同時想取隨機數睡覺，這 50 個人全都在搶著更新同一塊記憶體位址。其中一人搶贏，其他 49 人全部失敗並原地自旋（Spin）重新再搶。這導致執行緒還沒開始搶業務鎖，就先在「搶隨機數」上把 CPU 算力吃滿。
                // ThreadLocalRandom 的解法
                // Java 7 專門為了解決這個問題引入了 ThreadLocalRandom：
                // 隨機種子直接放在每個執行緒的 Thread 物件內部（私有變數）。
                // 執行緒只更新自己的記憶體，完全不碰別人的資源，徹底杜絕併發爭搶，效能完全吊打 Math.random()。
                TimeUnit.MILLISECONDS.sleep(ThreadLocalRandom.current().nextLong(100, 300));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }

        System.out.println("超過總預算時間，放棄操作！");
        return false;

    }

    static class Account {
        Lock lock;
        @Getter
        private int balance;

        public Account(int amount) {
            this.balance = amount;
            this.lock = new ReentrantLock();
        }

        public void checkout(int amount) {
            this.balance -= amount;
        }

        public void checkIn(int amount) {
            this.balance += amount;
        }
    }
}
