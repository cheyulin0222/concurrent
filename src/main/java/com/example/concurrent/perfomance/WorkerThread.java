package com.example.concurrent.perfomance;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

// 看似完全平行的架構，骨子裡其實藏著排隊堵塞的瓶頸（序列化，Serialization）
// Serialized 原本具備平行處理的能力，卻因為共用資源而「被壓扁成單線道」。
// 這個由平行被迫轉為循序排隊的過程與瓶頸，就叫做 Serialization（序列化）
public class WorkerThread extends Thread {

    // // 這不是建立一個新的佇列，這只是一個用來存「記憶體地址」的指標變數
    private final BlockingQueue<Runnable> queue;

    // 外部建立執行緒時，把同一個 queue 物件的地址傳進來
    public WorkerThread(BlockingQueue<Runnable> queue) {
        this.queue = queue;
    }

    @Override
    public void run() {
        while (true) {
            try {
                // 真實的瓶頸
                // queue 是所有 100 個執行緒共同看得一份清單
                // 為了避免「兩個執行緒同時拿走同一個任務」或「把佇列內部指標搞壞」，queue 內部一定有鎖（Lock）或同步化機制
                // 意味著：同一瞬間，只能有一個執行緒去拿任務
                Runnable task = queue.take();
                task.run();
            } catch (InterruptedException e) {
                break; /* Allow thread to exit */
            }
        }
    }

    public static void main(String[] args) {
        BlockingQueue<Runnable> queue = new LinkedBlockingQueue<>();

        WorkerThread t1 = new WorkerThread(queue);
        WorkerThread t2 = new WorkerThread(queue);
        WorkerThread t3 = new WorkerThread(queue);

        t1.start();
        t2.start();
        t3.start();
    }
}
