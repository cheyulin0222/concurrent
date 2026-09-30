package com.example.concurrent.self;

public class ConditionWaitExample {
    private final Object lock = new Object();
    // 狀態變數（以有界緩衝區為例：是否已滿）
    private boolean isFull = false;

    public void putItem(Object item) throws InterruptedException {
        // 1. acquire lock on object state
        // 進入 synchronized 區塊，先取得目標物件的內建鎖（Monitor Lock）
        synchronized (lock) {

            // 2. while (precondition does not hold)
            // 必須使用 while 迴圈而非 if，原因包含：
            // - 虛假喚醒（Spurious Wakeup）
            // - 被喚醒後、重新拿到鎖之前，狀態可能已被其他執行緒搶先改變
            while (isFull) {
                try {
                    // 3. release lock + wait + reacquire lock
                    // lock.wait() 底層同時完成三件事：
                    // a. 自動釋放目前持有的 lock
                    // b. 將當前執行緒掛起，加入該物件的條件佇列（Wait Set）
                    // c. 被 notify/notifyAll 喚醒後，自動重新競爭 lock，拿到後才從 wait() 返回
                    lock.wait();
                } catch (InterruptedException e) {
                    // 4. optionally fail if interrupted or timeout expires
                    // 等待期間若收到中斷訊號，退出等待並向上拋出例外或復原中斷狀態
                    Thread.currentThread().interrupt(); // 保持中斷旗標
                    throw e;
                }
            }

            // 5. perform action
            // 此時已重新持有鎖，且確認前置條件成立（!isFull）
            doPut(item);
            isFull = true;

            // 狀態已改變（由非滿變成可能有資料），喚醒其他正在等待「非空」條件的執行緒
            lock.notifyAll();
        }

        // 6. release lock
        // 離開 synchronized 區塊時，自動釋放 lock
    }

    private void doPut(Object item) throws InterruptedException {
        // 實際放入項目底層邏輯
    }

    public Object take() throws InterruptedException {
        synchronized (lock) {
            // 1. 如果空了，消費者就不能拿，進去 wait 睡覺並讓出鎖
            while (!isFull) {
                lock.wait();
            }

            // 2. 只有搶到鎖的人能執行到這裡：拿走物品
            Object item = doTake();

            // 拿走了一個東西，現在「絕對不是滿的」了！
            isFull = false;
            lock.notifyAll();

            return item;
        }
    }

    private Object doTake() {
        return new Object();
    }
}
