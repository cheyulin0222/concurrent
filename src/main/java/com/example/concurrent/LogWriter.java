package com.example.concurrent;

import java.io.PrintWriter;
import java.io.Writer;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

// 沒有關閉機制的 LogWriter
public class LogWriter {
    private final BlockingQueue<String> queue;
    private final LoggerThread logger;
    private static final int CAPACITY = 3;


    public LogWriter(PrintWriter writer) {
        this.queue = new LinkedBlockingQueue<>(CAPACITY);
        this.logger = new LoggerThread(writer);
    }

    public void start() {
        logger.start();
    }

    public void log(String msg) throws InterruptedException {
        queue.put(msg); // 生產者把訊息放著佇列
    }

    private class LoggerThread extends Thread {
        private final PrintWriter writer;

        public LoggerThread(PrintWriter writer) {
            this.writer = writer;
        }

        public void run() {
            try {
                // 如果沒有中斷它，背景執行緒不會結束，導致 JVM 無法關閉
                // 粗暴中斷會遺失資料與造成死鎖：如果直些 interrupt() 消費者執行緒
                // 訊息遺失：佇列中還沒寫完的日誌會被直接丟棄
                // 生產者永久阻塞：如果隊列當時是滿的，某些生產者執行緒正卡在 queue.put(msg) 等待空間
                // 此時消費者直接死亡，生產者將永遠等不到空間，永遠卡死
                while (true) {
                    writer.println(queue.take()); // 消費者從佇列取訊息寫入
                }
            } catch (InterruptedException ignored) {

            } finally {
                writer.close();
            }
        }
    }


}
