package com.example.concurrent;

import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Semaphore;

// 預設的 execute() 只會立刻接受任務或拋出例外
// 無法在「佇列滿了」時讓任務提交線程自動停下來等待 (Block)

public class BoundedExecutor {
    private final Executor exec;
    private final Semaphore semaphore;

    // bound = 線程池最大線程數 (Pool Size) + 允許佇列等待的任務數 (Queue Capacity)
    public BoundedExecutor(Executor exec, int bound) {
        this.exec = exec;
        // 初始化 Semaphore，設定上限 bound（例如：線程池大小 + 佇列容量）
        this.semaphore = new Semaphore(bound);
    }

    public void submitTask(final Runnable command) throws InterruptedException {
        // 1. 嘗試獲取許可（號碼牌）。如果許可已被拿光，這行會「阻塞等待」，直到有人釋放許可。
        semaphore.acquire();
        try {
            // 2. 將任務包裝後提交給真正的線程池執行
            exec.execute(() -> {
                try {
                    command.run(); // 真正執行使用者的任務
                } finally {
                    // 3. 任務執行完畢（無論成功或失敗），一定要釋放許可
                    semaphore.release();
                }
            });
        } catch (RejectedExecutionException e) {
            // 4. 如果線程池已關閉或拒絕接受任務，必須立刻釋放許可，否則 Semaphore 會漏卡（資源洩漏）
            // 歸還一張許可證。許可證數量 +1，並喚醒一個正在等待許可證的線程。
            semaphore.release();
        }
    }
}
