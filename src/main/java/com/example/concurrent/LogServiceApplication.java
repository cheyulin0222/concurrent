package com.example.concurrent;

import lombok.extern.slf4j.Slf4j;

import java.io.OutputStreamWriter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Slf4j
public class LogServiceApplication {
    public static void main(String[] args) throws InterruptedException {
        // 將日誌輸出至主控台 (Console)
        LogService logService = new LogService(new OutputStreamWriter(System.out));
        logService.start();

        System.out.println("=== 服務已啟動，開始多執行緒寫入日誌 ===");

        ExecutorService producers = Executors.newFixedThreadPool(5);
        for (int i = 1; i <= 10; i++) {
            final int taskId = i;
            producers.execute(() -> {
                try {
                    logService.log("Log Message #" + taskId + " from thread: " + Thread.currentThread().getName());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (IllegalStateException e) {
                    System.err.println("拒絕寫入: " + e.getMessage() + " (Task " + taskId + ")");
                }
            });
        }

        // 讓生產者發送一會兒
        Thread.sleep(100);

        System.out.println("\n>>> 發起關閉請求 logService.stop() <<<\n");
        logService.stop();

        // 嘗試在 stop() 後繼續寫入日誌，驗證是否會正確被拒絕
        try {
            logService.log("This message should be rejected!");
        } catch (IllegalStateException e) {
            System.out.println("【驗證成功】關閉後再呼叫 log() 成功拋出 IllegalStateException: " + e.getMessage());
        }

        producers.shutdown();
        producers.awaitTermination(5, TimeUnit.SECONDS);

        // 等待背景消費者寫完所有預留日誌並結束
        logService.awaitTermination();
        System.out.println("\n=== LogService 已成功優雅關閉，背景執行緒已退出 ===");
    }
}
