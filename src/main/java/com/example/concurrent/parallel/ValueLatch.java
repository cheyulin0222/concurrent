package com.example.concurrent.parallel;

import java.util.concurrent.CountDownLatch;

public class ValueLatch<T> {
    private T value = null;
    private final CountDownLatch done = new CountDownLatch(1);

    public boolean isSet() {
        return done.getCount() == 0;
    }

    public synchronized void setValue(T value) {
        if (!isSet()) {
            this.value = value;
            done.countDown();
        }
    }

    public T getValue() throws InterruptedException {
        done.await();
        synchronized (this) {
            return value;
        }
    }
}
