package com.example.concurrent.explicitlocks;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;
import java.util.concurrent.locks.Condition;

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
    public boolean tryLock(long timeout, TimeUnit unit) throws InterruptedException {
        return false;
    }

    @Override
    public void unlock() {

    }

    @Override
    public Condition newCondition() {
        return null;
    }

    abstract static class Sync extends AbstractQueuedSynchronizer {

        abstract boolean initialTryLock();

        final void lock() {
            if (!initialTryLock()) acquire(1);
        }
    }

    static final class NonfairSync extends Sync {

        @Override
        boolean initialTryLock() {
            Thread current = Thread.currentThread();
            if (compareAndSetState(0, 1)) {
                setExclusiveOwnerThread(current);
                return true;
            } else if (getExclusiveOwnerThread() == current) {
                int c = getState() + 1;
                if (c < 0) throw new Error("Maximum lock count exceed");
                setState(c);
                return true;
            } else return false;
        }
    }
}
