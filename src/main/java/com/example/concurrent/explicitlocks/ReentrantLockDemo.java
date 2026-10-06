package com.example.concurrent.explicitlocks;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ReentrantLockDemo {
    public void demo() {
        Lock lock = new ReentrantLock();

        lock.lock(); // 加鎖：置於 try 區塊之外
        try {
            // update object state
            // catch exception and restore invariants if necessary
        } finally {
            lock.unlock(); // 確保解鎖：必定會在退出時執行
        }
    }
}
