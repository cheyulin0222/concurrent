package com.example.concurrent.futurestudy;


import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

// 兌換券
// 當你把耗時任務丟給背景執行緒後，系統先還你一張 Future 收據
// 你可以拿著這張收據在未來隨時
// 1. 查詢進度
// 2. 取消任務
// 3. 憑券領取結果（若結果還沒出來，會在原地等待）
public interface Future<V> {

    // 1. 取消任務
    // mayInterruptIfRunning: 若任務已經在跑，是否發送中斷 (interrupt) 訊號強行叫停
    boolean cancel(boolean mayInterruptIfRunning);

    // 2. 查詢任務是否已被取消
    boolean isCanceled();

    // 3. 查詢任務是否已結束（成功完成、失敗拋例外、或被取消都算 true）
    boolean isDone();

    // 4. 阻塞（Block）等待並獲取結果
    // 如果任務還在跑，當前執行緒會卡住，直到任務完成回傳結果
    V get() throws InterruptedException, ExecutionException;

    // 5. 限時阻塞等待並獲取結果
    // 若超過指定時間還沒拿到結果，會拋出 TimeoutException，避免永久卡死
    V get(long timeout, TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException;
}
