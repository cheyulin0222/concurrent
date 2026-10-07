package com.example.concurrent.self;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CountDownLatchProject {
    
    public static void main(String[] args) throws InterruptedException {
        ExecutorService exec = Executors.newFixedThreadPool(3);

        // 2. 初始化門閂：倒數 3 次
        CountDownLatch latch = new CountDownLatch(3);

        long start = System.currentTimeMillis();

        // 任務 1：查詢使用者資料
        exec.submit(() -> {
            try {
                fetchUserInfo();
            } finally {
                latch.countDown(); // 保證發生異常也能扣減
            }
        });

        // 任務 2：查詢訂單紀錄
        exec.submit(() -> {
            try {
                fetchOrders();
            } finally {
                latch.countDown();
            }
        });

        // 任務 3：查詢會員點數
        exec.submit(() -> {
            try {
                fetchPoints();
            } finally {
                latch.countDown();
            }
        });

        System.out.println("主執行緒：" + Thread.currentThread().getName() + " 進入阻塞等待...");

        // 3. 主執行緒在此阻塞，等待 state 歸零
        latch.await();

        // 也可以加上超時防禦：latch.await(3, TimeUnit.SECONDS);

        long cost = System.currentTimeMillis() - start;
        System.out.println("3 個非同步查詢全部完成（耗時 " + cost + " ms），開始組裝資料回傳前端！");

        exec.shutdown();
    }

    private static void fetchUserInfo() {
        sleep(500);
        System.out.println(Thread.currentThread().getName() + " - 使用者資料查詢完畢");
    }

    private static void fetchOrders() {
        sleep(1000);
        System.out.println(Thread.currentThread().getName() + " - 訂單紀錄查詢完畢");
    }

    private static void fetchPoints() {
        sleep(800);
        System.out.println(Thread.currentThread().getName() + " - 會員點數查詢完畢");
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
