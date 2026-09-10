package com.example.concurrent.perfomance;

import java.util.concurrent.atomic.AtomicLong;

public class PageHitCounter {

    private final AtomicLong hits = new AtomicLong(0);

    public void increment() {
        hits.incrementAndGet();
    }

    public long getHits() {
        return hits.get();
    }
}
