package com.example.concurrent.explicitlocks;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;

// 將鎖的操作拆分為更細緻的控制方法，解決傳統 synchronized 一旦陷入阻塞就無法中斷或退出的問題
public interface Lock {
    // 無條件獲取鎖
    void lock();
    // 可中斷
    void lockInterruptibly() throws InterruptedException;
    // 輪詢嘗試
    boolean tryLock();
    // 超時嘗試
    boolean tryLock(long timeout, TimeUnit unit) throws InterruptedException;

    void unlock();
    Condition newCondition();

}
