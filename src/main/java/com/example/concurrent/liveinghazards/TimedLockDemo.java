package com.example.concurrent.liveinghazards;

import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class TimedLockDemo {

    private static final Lock lockA = new ReentrantLock();
    private static final Lock lockB = new ReentrantLock();
    private static final Random random = new Random();

    public static void main(String[] args) {
        Thread t1 = new Thread(() -> transfer(lockA, lockB, "Thread-1 (A -> B)"));

        Thread t2 = new Thread(() -> transfer(lockB, lockA, "Thread-2 (B -> A)"));

        t1.start();
        t2.start();
    }

    public static void transfer(Lock firstLock, Lock secondLock, String threadName) {
        while (true) {
            try {
                if (firstLock.tryLock(200, TimeUnit.MILLISECONDS)) {
                    try {
                        System.out.println(threadName + "：拿到第 1 把鎖，準備拿第 2 把鎖...");

                        Thread.sleep(100);

                        if (secondLock.tryLock(200, TimeUnit.MILLISECONDS)) {
                            try {
                                System.out.println("🎉 " + threadName + "：兩把鎖都拿到了！開始執行轉帳業務！");
                                return;
                            } finally {
                                // 業務做完，務必釋放第二把鎖
                                secondLock.unlock();
                                System.out.println(threadName + "：釋放了第 2 把鎖");
                            }
                        } else {
                            // 3. 核心精髓：搶不到第二把鎖！超時了！
                            System.out.println("⚠️ " + threadName + "：搶不到第 2 把鎖，放棄等待！準備釋放第 1 把鎖！");
                        }
                    } finally {
                        // 4. 無論是做完業務，還是搶不到第二把鎖，都要把第一把鎖釋放掉！
                        firstLock.unlock();
                        System.out.println(threadName + "：釋放了第 1 把鎖（退讓完畢）");
                    }
                } else {
                    System.out.println("⚠️ " + threadName + "：連第 1 把鎖都拿不到，稍後再試...");
                }

                // 5. 隨機休眠一小段時間（Backoff 退避策略），避免兩條執行緒像跳華爾滋一樣一直同頻撞在一起（活鎖 Livelock）
                Thread.sleep(random.nextInt(300) + 100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }
}
