package com.example.concurrent.test;

import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class TimedPutTakeTest {
    // -------------------------------------------------------------
    // 零件 1：計時器 (Listing 12.11)
    // -------------------------------------------------------------
    public static class BarrierTimer implements Runnable {
        private boolean started;
        private long startTime, endTime;

        public synchronized void run() {
            long t = System.nanoTime();
            if (!started) {
                started = true;
                startTime = t; // 第 1 次全員到齊（起跑點）：按下碼錶開始計時
            } else {
                endTime = t;   // 第 2 次全員到齊（終點線）：按下碼錶停止計時
            }
        }
        public synchronized void clear() { started = false; }
        public synchronized long getTime() { return endTime - startTime; }
    }

    // -------------------------------------------------------------
    // 零件 2：單場測試本體 (Listing 12.12 + 前面的 Producer/Consumer)
    // -------------------------------------------------------------
    private static final ExecutorService pool = Executors.newCachedThreadPool();
    private final AtomicInteger putSum = new AtomicInteger(0);
    private final AtomicInteger takeSum = new AtomicInteger(0);
    private final CyclicBarrier barrier;
    private final BoundedBuffer<Integer> bb;
    private final int nTrials, nPairs;
    private final BarrierTimer timer;

    public TimedPutTakeTest(int capacity, int npairs, int ntrials) {
        this.bb = new BoundedBuffer<>(capacity);
        this.nTrials = ntrials;
        this.nPairs = npairs;
        this.timer = new BarrierTimer();
        // 核心關鍵：把 timer 綁進柵欄！
        // 只要這 (npairs * 2 + 1) 個人全員到齊，柵欄就會自動去執行一次 timer.run()！
        this.barrier = new CyclicBarrier(npairs * 2 + 1, timer);
    }

    public void test() {
        try {
            timer.clear();
            for (int i = 0; i < nPairs; i++) {
                pool.execute(new Producer()); // 派生產者
                pool.execute(new Consumer()); // 派消費者
            }

            // 【第 1 次集合】大家在起跑線就位，全員到齊瞬間：timer.run() 自動觸發 -> 記下 startTime，開閘起跑！
            barrier.await();

            // 【第 2 次集合】大家把 10 萬次都做完、在終點集合，全員到齊瞬間：timer.run() 自動觸發 -> 記下 endTime！
            barrier.await();

            // 計算「平均每丟一個數字並拿出來花幾奈秒」
            long nsPerItem = timer.getTime() / (nPairs * (long)nTrials);
            System.out.print("Throughput: " + nsPerItem + " ns/item");

            // 確保資料依然沒算錯（測試不能測出快速的垃圾資料）
            if (putSum.get() != takeSum.get()) {
                throw new AssertionError("資料損毀！存入與取出總額不符！");
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // 生產者與消費者內部類別
    class Producer implements Runnable {
        public void run() {
            try {
                int seed = (this.hashCode() ^ (int)System.nanoTime());
                int sum = 0;
                barrier.await(); // 到起跑線等
                for (int i = nTrials; i > 0; --i) {
                    bb.put(seed);
                    sum += seed;
                    seed = xorShift(seed);
                }
                putSum.getAndAdd(sum);
                barrier.await(); // 到終點線等
            } catch (Exception e) { throw new RuntimeException(e); }
        }
    }

    class Consumer implements Runnable {
        public void run() {
            try {
                barrier.await(); // 到起跑線等
                int sum = 0;
                for (int i = nTrials; i > 0; --i) {
                    sum += bb.take();
                }
                takeSum.getAndAdd(sum);
                barrier.await(); // 到終點線等
            } catch (Exception e) { throw new RuntimeException(e); }
        }
    }

    static int xorShift(int y) {
        y ^= (y << 6);
        y ^= (y >>> 21);
        y ^= (y << 7);
        return y;
    }

    // -------------------------------------------------------------
    // 零件 3：總指揮主程式 (Listing 12.13)
    // -------------------------------------------------------------
    public static void main(String[] args) throws Exception {
        int tpt = 100000; // 每個執行緒跑 10 萬次

        // 外層迴圈：測試不同 Buffer 容量 (1, 10, 100, 1000)
        for (int cap = 1; cap <= 1000; cap *= 10) {
            System.out.println("=== 測試緩衝區容量: " + cap + " ===");

            // 內層迴圈：測試不同工人對數 (1對, 2對, 4對 ... 128對)
            for (int pairs = 1; pairs <= 128; pairs *= 2) {
                TimedPutTakeTest t = new TimedPutTakeTest(cap, pairs, tpt);
                System.out.print("工人對數: " + pairs + "\t");
                t.test(); // 跑一次測試
                System.out.println();
                Thread.sleep(1000); // 讓 CPU 休息 1 秒再測下一輪
            }
        }
        pool.shutdown();
    }
}
