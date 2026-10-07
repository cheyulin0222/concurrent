package com.example.concurrent.self;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class SemaphoreProject {
    public static void main(String[] args) throws InterruptedException {
        // 1. 建立一個只有 2 個許可證的信號量
        Semaphore semaphore = new Semaphore(2);

        Runnable task = () -> {
            try {
                semaphore.acquire(); // // 拿取許可證（若已被拿光，當前執行緒在此阻塞排隊）
                System.out.println(Thread.currentThread().getName() + " 正在呼叫外部 API...");
                Thread.sleep(2000);   // 模擬業務耗時
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                System.out.println(Thread.currentThread().getName() + " 完成任務，歸還許可證");
                semaphore.release();
            }
        };

        // 3. 建立一個可容納 6 條執行緒的執行緒池
        ExecutorService exec = Executors.newFixedThreadPool(6);

        // 4. 同時提交 6 個任務
        for (int i = 1; i <= 6; i++) {
            exec.submit(task);
        }

        // 5. 優雅關閉執行緒池
        exec.shutdown();
        exec.awaitTermination(15, TimeUnit.SECONDS);
    }
}
