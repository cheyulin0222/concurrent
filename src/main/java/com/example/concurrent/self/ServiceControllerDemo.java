package com.example.concurrent.self;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ServiceControllerDemo {

    // 業務控制器：封裝 ThreadGate 來控制所有 Worker 的啟動與暫停
    public static class TaskProcessor {
        private final ThreadGate gate = new ThreadGate();

        public TaskProcessor() {
            // 初始狀態：直接開門，讓所有任務正常流通
            gate.open();
        }

        // 維護人員呼叫：暫停系統（關門）
        public void pause() {
            System.out.println("\n[系統號令] === 暫停所有工作 (Gate Closed) ===");
            gate.close();
        }

        // 維護人員呼叫：恢復系統（開門放行）
        public void resume() {
            System.out.println("\n[系統號令] === 恢復所有工作 (Gate Opened) ===");
            gate.open();
        }

        public void processTask(int workerId, int taskId) throws InterruptedException {
            // 【關鍵檢查點】：每個任務執行前，先到閘門前確認能不能走
            // 如果門開著，直接秒過；如果門被 pause() 關上了，執行緒會在此沉睡
            gate.await();

            // 真正的業務邏輯
            System.out.printf("Worker-%d 正在處理任務 [%d]%n", workerId, taskId);
            Thread.sleep(300); // 模擬耗時處理
        }
    }

    public static void main(String[] args) throws InterruptedException {
        TaskProcessor processor = new TaskProcessor();
        ExecutorService pool = Executors.newFixedThreadPool(3);

        // 啟動 3 個 Worker 不斷領取任務
        for (int i = 1; i <= 3; i++) {
            final int workerId = i;
            pool.submit(() -> {
                try {
                    int taskId = 1;
                    while (!Thread.currentThread().isInterrupted()) {
                        processor.processTask(workerId, taskId++);
                    }
                } catch (InterruptedException e) {
                    System.out.printf("Worker-%d 收到終止訊號，退出。%n", workerId);
                }
            });
        }

        // 1. 讓系統正常運作 1 秒（Worker 持續跑任務）
        Thread.sleep(1000);

        // 2. 模擬線上運維：按下暫停（熱更新或調整配置）
        processor.pause();
        System.out.println(">>> 系統已暫停，等待 2 秒（此時 Worker 不會有任何新輸出）...");
        Thread.sleep(2000);

        // 3. 配置更新完成：恢復運作
        processor.resume();
        Thread.sleep(1000);

        // 4. 再演示一次：再次暫停與恢復（展現可重複使用的能力）
        processor.pause();
        Thread.sleep(1500);
        processor.resume();
        Thread.sleep(1000);

        // 結束程式
        pool.shutdownNow();
        pool.awaitTermination(3, TimeUnit.SECONDS);
    }
}
