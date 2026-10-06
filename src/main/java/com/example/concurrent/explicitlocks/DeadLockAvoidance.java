package com.example.concurrent.explicitlocks;

import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.NANOSECONDS;

public class DeadLockAvoidance {

    public boolean transferMoney(Account from, Account to , int amount, long timeout, TimeUnit unit) {
        long fixedDelay = getFixedDelayComponentNanos(timeout, unit);
        long randMod = getRandomDelayModulesNanos(timeout, unit);
        long stopTime = System.nanoTime() + unit.toNanos(timeout);

        while (true) {
            // 輪詢嘗試，如果拿不到不會死等，直接跳出 if
            if (from.lock.tryLock()) {
                try {
                    // 第二把鎖獲取失敗立刻放手 (Back-off)
                    if (to.lock.tryLock()) {
                        try {
                            if (from.getBalance().compareTo(amount) < 0) throw new RuntimeException("餘額不足");
                            else {
                                from.debit(amount);
                                to.credit(amount);
                                return true;
                            }
                        } finally {
                            to.lock.unlock();
                        }
                    }
                // 關鍵點：若 to 拿不到，進入 from 的 finally 區塊，立刻 unlock
                } finally {
                    from.lock.unlock();
                }
            }
            if (System.nanoTime() > stopTime) return false;
            NANOSECONDS.sleep(fixedDelay + rnd.nextLong() % randMod);
        }
    }

    class Account {
        Lock lock = new ReentrantLock();
        Integer balance;

        Integer getBalance() {
            return balance;
        }

        void debit(int amount) {
            this.balance -= amount;
        }

        void credit(int amount) {
            this.balance += amount;
        }

    }
}
