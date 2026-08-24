package com.example.concurrent;

public class PrivateLock {
    private final Object myLock = new Object();

    Widget widget;

    void someMethod() {
        synchronized (myLock) {
            // Access or modify the state of widget
        }
    }
}
