package com.example.concurrent.self;

// 一般的 CountDownLatch 是一次性的（開門後就無法再關閉）。
// CyclicBarrier 可以重複使用
// 但它是「湊齊固定人數自動放行」，外部控制者無法手動、任意地決定何時暫停、何時放行

// 提供一個由外部主動、隨時 暫停 與 恢復 業務執行的控制開關
public class ThreadGate {
    private boolean isOpen;
    private int generation;

    public synchronized void close() {
        isOpen = false;
    }

    public synchronized void open() {
        ++generation;
        isOpen = true;
        notifyAll();
    }

    public synchronized void await() throws InterruptedException {
        int arrivalGeneration = generation;
        while (!isOpen && arrivalGeneration == generation) wait();
    }
}
