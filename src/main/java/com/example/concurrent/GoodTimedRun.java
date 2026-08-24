package com.example.concurrent;

import java.util.concurrent.TimeUnit;

public class GoodTimedRun {

    public static void timedRun(final Runnable r, long timeout, TimeUnit unit) throws InterruptedException {

    }

    public void handleUserOrder() throws InterruptedException {
        timedRun(this::calculateComplexDiscount, 2, TimeUnit.SECONDS);
        
        processPaymentAndSaveOrder();
        
    }

    private void processPaymentAndSaveOrder() {
    }

    private void calculateComplexDiscount() {
    }
}
