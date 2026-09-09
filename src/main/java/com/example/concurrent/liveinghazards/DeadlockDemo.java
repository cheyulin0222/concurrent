package com.example.concurrent.liveinghazards;

public class DeadlockDemo {
    private static final Object lockA = new Object();
    private static final Object lockB = new Object();

    public static void main(String[] args) {
        // 執行緒 1：先拿 A，再要 B
        new Thread(() -> {
            synchronized (lockA) {
                System.out.println("Thread-1: 拿到 lockA，正在等 lockB...");
                try { Thread.sleep(100); } catch (InterruptedException ignored) {}
                synchronized (lockB) {
                    System.out.println("Thread-1: 拿到 lockB！");
                }
            }
        }, "Deadlock-Thread-1").start();

        // 執行緒 2：先拿 B，再要 A
        new Thread(() -> {
            synchronized (lockB) {
                System.out.println("Thread-2: 拿到 lockB，正在等 lockA...");
                try { Thread.sleep(100); } catch (InterruptedException ignored) {}
                synchronized (lockA) {
                    System.out.println("Thread-2: 拿到 lockA！");
                }
            }
        }, "Deadlock-Thread-2").start();
    }
}
