package com.example.concurrent;

import java.io.PrintWriter;
import java.io.Writer;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class LogService {
    private final BlockingQueue<String> queue;
    private final LoggerThread loggerThread;
    private final PrintWriter writer;
    private boolean isShutdown;
    private int reservation; // 預留計數器
    private static final int CAPACITY = 3;

    public LogService(Writer writer) {
        // 使用 PrintWriter 並開啟 autoFlush (自動刷寫流)
        this.writer = new PrintWriter(writer, true);
        this.queue = new LinkedBlockingQueue<>(CAPACITY);
        this.loggerThread = new LoggerThread();
    }

    /**
     * 啟動背景日誌消費執行緒
     */
    public void start() {
        loggerThread.start();
    }

    /**
     * 停止日誌服務（優雅關閉）
     */
    public void stop() {
        synchronized (this) {
            isShutdown = true;
        }
        // 喚醒可能卡在 queue.take() 阻塞等待的消費者執行緒
        loggerThread.interrupt();
    }

    /**
     * 等待背景執行緒真正結束（方便測試與外部管理）
     */
    public void awaitTermination() throws InterruptedException {
        loggerThread.join();
    }

    /**
     * 生產者寫入日誌
     */
    public void log(String msg) throws InterruptedException {
        synchronized (this) {
            if (isShutdown) throw new IllegalStateException("Logger is shut down");
            ++reservation; // 原子化：檢查狀態並 預留 寫入額度
        }

        try {
            queue.put(msg);
        } catch (InterruptedException e) {
            // 【修復 1】：若 put 操作被打斷，必須回滾預留額度，否則背景執行緒會永遠等不到 reservation 歸零而卡死
            synchronized (this) {
                --reservation;
            }
            throw e;
        }

    }

    private class LoggerThread extends Thread {
        @Override
        public void run() {
            try {
                while (true) {
                    try {
                        synchronized (LogService.this) {
                            // 核心判定：只有當「發起關閉」且「所有預留的訊息都處理完畢」時，才退出迴圈
                            if (isShutdown && reservation == 0) break;
                        }

                        // 取出訊息（若隊列為空且未關閉，此處會阻塞等待）
                        String msg = queue.take();


                        synchronized (LogService.this) {
                            --reservation;  // 完成一筆，預留額度減 1
                        }
                        writer.println(msg);
                    } catch (InterruptedException e) {
                        /* 吞掉中斷，重試迴圈檢查 (isShutdown && reservation == 0) */
                    }
                }
            } finally {
                // 【修復 2】：只刷寫緩衝區，不要 close()！避免關閉主控台的 System.out
                writer.flush();
            }
        }
    }
}
