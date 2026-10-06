package com.example.concurrent.aqs;

public class AbstractQueuedSynchronizer {
    private volatile int state;
    private transient volatile Node head;
    private transient volatile Node tail;

    abstract static class Node {
        volatile Node prev;
        volatile Node next;
        Thread waiter;
        volatile int status;
    }
}
