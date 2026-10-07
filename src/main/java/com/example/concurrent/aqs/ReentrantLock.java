package com.example.concurrent.aqs;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

// 可重入」的意思是：同一個執行緒，在已經拿到鎖的情況下
// 可以「再次進入」同一個鎖保護的其他程式碼區塊，而不會把自己卡死。
public class ReentrantLock implements Lock {

    private final Sync sync;

    public ReentrantLock() {
        sync = new NonfairSync();
    }



    @Override
    public void lock() {
        sync.lock();
    }

    @Override
    public void lockInterruptibly() throws InterruptedException {

    }

    @Override
    public boolean tryLock() {
        return false;
    }

    @Override
    public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
        return false;
    }

    @Override
    public void unlock() {
        sync.release(1);
    }

    @Override
    public Condition newCondition() {
        return null;
    }

    static final class NonfairSync extends Sync {
        final boolean initialTryLock() {
            Thread current = Thread.currentThread();
            // 1. 完全不看隊列有沒有人在排隊，直接嘗試 CAS 把 state 從 0 改成 1
            if (compareAndSetState(0, 1)) { // first attempt is unguarded
                // 搶到鎖，把自己設為擁有者
                setExclusiveOwnerThread(current);
                return true;
            // 2. 如果沒搶到，檢查是不是自己「重入」了這把鎖
            } else if (getExclusiveOwnerThread() == current) {
                int c = getState() + 1;
                if (c < 0) // overflow
                    throw new Error("Maximum lock count exceeded");
                setState(c);
                return true;
            // 3. 既沒搶到，也不是重入，搶鎖失敗
            } else return false;
        }

        protected final boolean tryAcquire(int acquires) {
            if (getState() == 0 && compareAndSetState(0, acquires)) {
                setExclusiveOwnerThread(Thread.currentThread());
                return true;
            }
            return false;
        }
    }


    abstract static class Sync extends AbstractQueuedSynchronizer {
        final boolean tryLock() {
            Thread current = Thread.currentThread();
            int c = getState();
            if (c == 0) {
                if (compareAndSetState(0, 1)) {
                    setExclusiveOwnerThread(current);
                    return true;
                }
            } else if (getExclusiveOwnerThread() == current) {
                if (++c < 0) // overflow
                    throw new Error("Maximum lock count exceeded");
                setState(c);
                return true;
            }
            return false;
        }

        protected final boolean tryRelease(int releases) {
            // c 代表「扣減這次釋放後，該執行緒還持有幾層鎖」。
            int c = getState() - releases;
            // 安全檢查：防止「沒拿到鎖的人跑來放鎖」
            if (getExclusiveOwnerThread() != Thread.currentThread())
                throw new IllegalMonitorStateException();
            boolean free = (c == 0);
            // 判斷鎖是否「徹底空了」
            if (free)
                setExclusiveOwnerThread(null);
            setState(c);
            return free;
        }

        abstract boolean initialTryLock();

        final void lock() {
            if (!initialTryLock())
                acquire(1);
        }

    }
}
