package com.example.concurrent.aqs;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
public class FlashSaleService {

    // 預設為非公平鎖（Non-fair Lock，吞吐量最高）
    private final Lock lock = new ReentrantLock();
    private int stock = 1; // 假設只剩下最後 1 件 iPhone 限量特價

    public boolean buyProduct(String customerName) {
        System.out.printf("[%s] 來到收銀台準備搶購...%n", customerName);

        // 【進入點】：執行緒呼叫 lock()
        lock.lock();
        try {
            System.out.printf(">>> [%s] 成功搶到鎖，開始扣減庫存！%n", customerName);
            if (stock > 0) {
                Thread.sleep(100); // 模擬寫入資料庫耗時
                stock--;
                System.out.printf("🎉 [%s] 購買成功！剩餘庫存：%d%n", customerName, stock);
                return true;
            } else {
                System.out.printf("❌ [%s] 庫存不足，購買失敗。%n", customerName);
                return false;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } finally {
            // 【退出點】：執行緒呼叫 unlock()
            System.out.printf("<<< [%s] 釋放鎖，離開收銀台。%n", customerName);
            lock.unlock();
        }
    }
}
