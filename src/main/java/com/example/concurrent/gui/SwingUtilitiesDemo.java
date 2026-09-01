package com.example.concurrent.gui;

import java.util.concurrent.*;

public class SwingUtilitiesDemo {
    // 1. 底層使用單一執行緒的 Executor，相當於 Swing 的專屬事件執行緒 (EDT)
    private static final ExecutorService exec = Executors.newSingleThreadExecutor(new SwingThreadFactory());

    private static volatile Thread swingThread;

    // 自訂 ThreadFactory，用來記錄建立出來的這唯一的「GUI 執行緒」是哪一個
    private static class SwingThreadFactory implements ThreadFactory {
        @Override
        public Thread newThread(Runnable r) {
            swingThread = new Thread(r, "My_Custom-EDT");
            return swingThread;
        }
    }

    // 判斷當前呼叫此方法的執行緒，是否就是那唯一的「GUI 執行緒」
    public static boolean isEventDispatchThread() {
        return Thread.currentThread() == swingThread;
    }

    // 非同步執行：將任務排入單一執行緒佇列，呼叫者不用等待直接繼續
    public static void invokeLater(Runnable task) {
        exec.execute(task);
    }

    // 同步執行：將任務排入單一執行緒佇列，並「阻塞（Block）」當前執行緒，直到該任務執行完畢
    public static void invokeAndWait(Runnable task) throws InterruptedException, ExecutionException {
        Future<?> f = exec.submit(task);
        f.get(); // f.get() 會卡住目前執行緒，直到 exec 裡的 task 執行完成
    }

    public static void shutdown() {
        exec.shutdown();
    }

    static void main() {
        System.out.println("[" + Thread.currentThread().getName() + "] 主程式開始執行...");

        try {
            SwingUtilitiesDemo.invokeLater(() -> {
                System.out.println("[" + Thread.currentThread().getName() + "] 正在執行 invokeLater 任務");
                System.out.println("  -> 當前是 Event Dispatch Thread 嗎？ " + SwingUtilitiesDemo.isEventDispatchThread());
            });

            // 測試 2：使用 invokeAndWait (同步)
            System.out.println("[" + Thread.currentThread().getName() + "] 主執行緒準備呼叫 invokeAndWait，即將被卡住...");

            SwingUtilitiesDemo.invokeAndWait(() -> {
                try {
                    // 模擬一個耗時 1 秒的 GUI 任務
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                System.out.println("[" + Thread.currentThread().getName() + "] 正在執行 invokeAndWait 任務");
                System.out.println("  -> 當前是 Event Dispatch Thread 嗎？ " + SwingUtilitiesDemo.isEventDispatchThread());
            });
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        } finally {
            SwingUtilitiesDemo.shutdown();
        }
    }
}
