package com.example.concurrent.self;

// 雖然 SleepyBoundedBuffer 比第一種動不動就噴例外的做法好很多
// 但本質上它仍然是盲目的定時輪詢。
public class SleepyBoundedBuffer<V> extends BaseBoundedBuffer<V> {

    // 每次條件不滿足時，執行緒休眠的時間間隔（毫秒）
    private static final long SLEEP_GRANULARITY = 50;

    public SleepyBoundedBuffer(int capacity) {
        super(capacity);
    }

    public void put(V v) throws InterruptedException {
        // 不斷輪詢直到條件成立
        while (true) {
            // 1. 取得物件鎖，以安全地檢查與修改狀態
            synchronized (this) {
                if (!isFull()) {
                    doPut(v);   // 條件符合，放入元素
                    return;     // 完成操作並離開方法（此時會自動釋放鎖）
                }
            }   // 離開 synchronized 區塊，【鎖在此處被釋放】

            // 2. 條件不成立時休眠（此時沒有持有鎖！）
            // 讓出 CPU 並讓其他消費者有機會搶鎖進來拿資料、改變狀態
            // 若此時收到中斷訊號，sleep 會拋出 InterruptedException 並退出
            Thread.sleep(SLEEP_GRANULARITY);
        }
    }

    public V take() throws InterruptedException {
        while (true) {
            // 1. 取得物件鎖檢查緩衝區是否非空
            synchronized (this) {
                if (!isEmpty()) {
                    return doTake();    // 條件符合，取出元素並返回（自動釋放鎖）
                }
            }   // 離開 synchronized 區塊，【鎖在此處被釋放】

            // 2. 緩衝區仍為空，釋放鎖後休眠一小段時間再重試
            Thread.sleep(SLEEP_GRANULARITY);
        }
    }
}
