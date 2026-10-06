package com.example.concurrent.test;

public class BarrierTimer implements Runnable {
    private boolean started;
    private long startTime, endTime;


    @Override
    public synchronized void run() {
        long t = System.nanoTime();
        if (!started) {
            started = true;
            startTime = t;  // 第 1 次全員到齊（起跑）：記下開始時間
        } else {
            endTime = t;    // 第 2 次全員到齊（終點）：記下結束時間
        }
    }

    public synchronized void clear() {
        started = false;
    }

    public synchronized long getTime() {
        return endTime - startTime;
    }
}
