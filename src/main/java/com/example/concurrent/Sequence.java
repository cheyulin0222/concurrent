package com.example.concurrent;


public class Sequence {

    private int value;

    public synchronized int getNext() {
        return value++;
    }
}
