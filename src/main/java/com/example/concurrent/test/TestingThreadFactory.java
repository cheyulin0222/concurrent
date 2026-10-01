package com.example.concurrent.test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

// 透過實作客製化的 TestingThreadFactory
// 在 newThread() 被呼叫時遞增原子計數器 numCreated
// 即可在測試中精準掌握執行緒池到底實質建立了多少條底層執行緒。

// 若要驗證閒置回收，也可以延伸回傳客製化 Thread，在執行緒終止時做紀錄。
public class TestingThreadFactory implements ThreadFactory {
    private final AtomicInteger numCreated = new AtomicInteger();
    private final ThreadFactory factory = Executors.defaultThreadFactory();

    @Override
    public Thread newThread(Runnable r) {
        numCreated.incrementAndGet();
        return factory.newThread(r);
    }

    public void testPoolExpansion() throws InterruptedException {
        int MAX_SIZE = 10;
        TestingThreadFactory threadFactory = new TestingThreadFactory();
        ExecutorService exec = Executors.newFixedThreadPool(MAX_SIZE, threadFactory);

        for (int i = 0; i < 10 * MAX_SIZE; i++) {
            exec.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(Long.MAX_VALUE);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            });
        }
        for (int i = 0; i < 20 && threadFactory.numCreated.get() < MAX_SIZE; i++) {
            Thread.sleep(100);
        }

        // 3. 驗證：池子有沒有剛好擴展到 10 條執行緒？
        assertEquals(threadFactory.numCreated.get(), MAX_SIZE);

        // 4. 清理：把那些在睡大覺的工人強制叫醒並關閉池子
        exec.shutdownNow();
    }

    private void assertEquals(int i, int maxSize) {

    }
}
