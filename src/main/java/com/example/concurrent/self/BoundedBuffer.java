package com.example.concurrent.self;

// BoundedBuffer 雖然能正常運作
// 因為它的狀態只有「滿」和「空」兩個簡單旗標
// 當你想要把這套邏輯搬去實作一個「可以隨時開門、關門的閘門（Gate）」時，原本這套寫法會瞬間爆出嚴重 Bug！
public class BoundedBuffer<V> extends BaseBoundedBuffer<V> {

    // 條件謂詞（Condition Predicate）：
    // 放入的前置條件：非滿 (!isFull())
    // 取出的前置條件：非空 (!isEmpty())
    public BoundedBuffer(int capacity) {
        super(capacity);
    }

    // BLOCKS-UNTIL: not-full（阻塞直到非滿）
    public synchronized void put(V v) throws InterruptedException {
        // 1. 必須使用 while 迴圈檢查狀態
        // 醒來後可能被其他人搶先填滿，或者遭遇虛假喚醒（Spurious Wakeup）
        while (isFull()) {
            // 2. 條件不滿足，自動釋放 this 鎖，當前執行緒加入 this 的 wait set 進入沉睡
            // 當被 notifyAll() 叫醒並重新奪回 this 鎖時，才會從這裡返回並繼續走迴圈
            wait();
        }

        // 3. 此時保證持有鎖且 !isFull()，真正將元素存入
        doPut(v);

        // 4. 重要！狀態改變了（現在裡面至少有 1 個元素，即「非空」條件成立）
        // 喚醒所有在 wait set 中等待的執行緒（包含可能正在等非空的消費者）
        notifyAll();
    }

    // BLOCKS-UNTIL: not-empty（阻塞直到非空）
    public synchronized V take() throws InterruptedException {
        // 1. 檢查是否為空，若是空的就不能拿
        while (isEmpty()) {
            // 自動釋放鎖，進入等待佇列
            wait();
        }

        // 2. 重新奪回鎖且確認 !isEmpty()，取出元素
        V v = doTake();

        // 3. 重要！狀態改變了（現在裡面至少有 1 個空位，即「非滿」條件成立）
        // 喚醒所有正在等「有空位（非滿）」的生產者執行緒
        notifyAll();

        return v;
    }


}
