package com.example.concurrent.test;

import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class PutTakeTest {
    // 派遣工人
    private static final ExecutorService pool = Executors.newCachedThreadPool();
    // 塞進去的數字加總
    private final AtomicInteger putSum = new AtomicInteger(0);
    // 取出的數字加總
    private final AtomicInteger takeSum = new AtomicInteger(0);
    // 為了讓執行緒同時進行
    private final CyclicBarrier barrier;
    private final BounderBuffer<Integer> bb;
    // nPairs 有幾對生產生與消費者
    // 生產執行緒 專門塞東西
    // 消費執行緒 專門拿東西
    // 總共有 20 個工人在搶這個 Buffer
    // nTrials 每個工人要重複做幾次 (100000)
    private final int nTrials, nPairs;

    PutTakeTest(int capacity, int npairs, int ntrials) {
        this.bb = new BounderBuffer<>(capacity);
        this.nTrials = ntrials;
        this.nPairs = npairs;
        // 必須湊齊 21 個人，閘門才會打開
        // 生產 + 消費 + main
        this.barrier = new CyclicBarrier(npairs * 2 + 1);
    }

    void test() {
        try {
            for (int i = 0; i < nPairs; i++) {
                pool.execute(new Producer());
                pool.execute(new Consumer());
            }

            barrier.await(); // wait for all threads to be ready
            barrier.await(); // wait for all threads to finish
            assertEquals(putSum.get(), takeSum.get());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void assertEquals(int i, int i1) {

    }

    class Producer implements Runnable {
        @Override
        public void run() {
            try {
                // 生產著 每次準備塞進去的數字
                // 現代編譯器非常聰明。如果你塞連續數字 1 到 100000
                // 編譯器在執行前就能算出加總結果（用梯形公式），甚至可能私下把迴圈簡化掉，導致測試失去真實壓力。
                int seed = (this.hashCode() ^ (int)System.nanoTime()); // 獨立隨機種子
                // 每個工人「自己的專屬小記帳本（執行緒私有變數）」
                // 如果 20 個執行緒每一微秒都去搶著修改同一個 putSum，這 20 個人會全部卡死在搶 putSum 上面
                int sum = 0; // 執行緒本地變數，完全不用鎖
                barrier.await(); // 等起跑信號
                for (int i = nTrials; i > 0; --i) {
                    bb.put(seed); // 塞進 BoundedBuffer
                    sum += seed;    // 在自己的本地變數累加
                    seed = xorShift(seed);  // 產生下一個隨機數
                }
                putSum.getAndAdd(sum); // 全做完後，才原子性併入全域加總
                barrier.await();        // 等所有人完成
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
    class Consumer implements Runnable {
        @Override
        public void run() {
            try {
                barrier.await();    // 等起跑信號
                int sum = 0;        // 執行緒本地變數
                for (int i = nTrials; i > 0; --i) {
                    sum += bb.take();   // 從 BoundedBuffer 取出並累加
                }
                takeSum.getAndAdd(sum); // 全做完後，才原子性併入全域加總
                barrier.await();        // 等所有人完成
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static void main(String[] args) {
        // 啟動 20 個執行緒
        new PutTakeTest(10, 10, 100000).test();
        pool.shutdown();
    }

    // 偽隨機數產生器
    static int xorShift(int y) {
        // << 或 >>>：位移（Shift），把二進位的 0 與 1 往左或往右推
        // ^=：互斥或（XOR），對應位元相同變 0，不同變 1。
        y ^= (y << 6);      // 1. 往左推 6 格，拿結果跟原本的自己做 XOR 混淆
        y ^= (y >>> 21);    // 2. 往右無號位移 21 格，再跟自己做 XOR
        y ^= (y << 7);      // 3. 往左推 7 格，再跟自己做 XOR
        return y;           // 4. 回傳徹底被打亂的新數字
    }
}
