package com.example.concurrent;

import java.io.PrintWriter;
import java.io.Writer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

public class SimplifyLogService {
    private final ExecutorService exec = Executors.newSingleThreadExecutor();
    private final PrintWriter writer;

    public SimplifyLogService(Writer writer) {
        this.writer = new PrintWriter(writer, true);
    }

    public void log(String msg) {
        try {
            // 完全不用 synchronized、不用 check isShutdown、不用 ++reservation
            exec.execute(() -> writer.println(msg));
        } catch (RejectedExecutionException e) {
            // exec.shutdown() 後再 execute，Executor 會自動拋出這個例外！
            throw new IllegalStateException("Logger is shut down");
        }
    }

    public void stop() throws InterruptedException {
        try {
            exec.shutdown(); // 1. 停止接受新任務，但會把佇列中已有的任務全部處理完
            exec.awaitTermination(30, TimeUnit.SECONDS); // 2. 等待背景執行緒處理完畢退出
        } finally {
            writer.flush();
        }
    }
}
