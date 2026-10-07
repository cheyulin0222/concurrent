package com.example.concurrent.nonblocking;

public class SimulateCAS {

    private int value;

    public synchronized int get() {
        return value;
    }

    public synchronized int compareAndSwap(int expectedValue, int newValue) {
        int oldValue = get();

        if (oldValue == expectedValue)
            value = newValue;

        return oldValue;
    }

    public synchronized boolean compareAndSet(int expectedValue, int newValue) {
        return (expectedValue == compareAndSwap(expectedValue, newValue));
    }

}
