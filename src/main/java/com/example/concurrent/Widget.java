package com.example.concurrent;

public class Widget {
    public synchronized void doSomething() {

    }

    public static class LoggingWidget extends Widget {
        public synchronized void doSomething() {
            System.out.println(toString() + ": calling doSomething");
            super.doSomething();
        }
    }
}
