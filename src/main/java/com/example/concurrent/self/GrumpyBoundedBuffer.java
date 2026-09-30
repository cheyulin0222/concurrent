package com.example.concurrent.self;

// 只要發現前置條件不符合（滿了不能放、空了不能拿）
// 立刻拋出例外，把問題甩鍋給呼叫者自己去處理
// 產生三大問題
// 1. 濫用例外機制（Abusing Exceptions）
// 例外應該只用在「異常情況（Exceptional conditions）」，絕不能拿來控制正常的業務流程。

// 2. 呼叫端代碼變得極度醜陋且痛苦(Listing 14.4)
// 因為這個類別直接甩鍋，所有使用它的人都必須寫成這種醜陋的重試迴圈：

// 3. 陷入「CPU 飆高」與「反應延遲」的兩難（Spinning vs. Sleeping）
// 不睡覺硬狂轉（Busy waiting / Spin waiting）：
// 如果迴圈裡不呼叫 Thread.sleep() 一直死命 take()，會把 CPU 單核直接打到 100%，浪費極多電量與運算資源。
//
// 睡覺（Thread.sleep）：
// 如果設睡 50ms，結果你剛閉眼 1ms 別人就放進資料了，你卻要白白「睡過頭（Oversleep）」剩下的 49ms，導致系統反應遲鈍（Poor responsiveness）。
//
// 呼叫 Thread.yield()：
// 稍微折衷一點（告訴 CPU 我讓出時間片給別人先跑），但依然本質上是輪詢，無法從根源解決問題。

// 4. 破壞了公平性（FIFO 被打亂)
// 把等待的責任丟給外部呼叫者重試，就無法維持「誰先來誰先拿（FIFO）」的順序。
// 原本 Thread A 最先來等，結果它剛好睡著了；
// 這時剛來的 Thread B 運氣好直接重試成功，等於插隊，徹底失去了公平排隊的機制。
public class GrumpyBoundedBuffer<V> extends BaseBoundedBuffer<V> {

    public GrumpyBoundedBuffer(int capacity) {
        super(capacity);
    }

    public synchronized void put(V v) throws BufferFullException {
        // 滿了直接甩鍋拋出例外，完全不等待
        if (isFull()) throw new BufferFullException();
        doPut(v);
    }

    public synchronized V take() throws BufferEmptyException {
        // 空了直接甩鍋拋出例外，完全不等待
        if (isEmpty()) throw new BufferEmptyException();
        return doTake();
    }

    // Listing 14.4
    static void main(String[] args) throws InterruptedException {
        GrumpyBoundedBuffer<Object> buffer = new GrumpyBoundedBuffer<>(5);

        while (true) {
            try {
                Object item = buffer.take();
                break;
            } catch (BufferEmptyException e) {
                Thread.sleep(5);
            }
        }
    }
}
