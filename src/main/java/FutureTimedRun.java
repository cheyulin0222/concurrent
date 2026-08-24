import java.util.concurrent.*;

public class FutureTimedRun {

    private static final ExecutorService taskExec = Executors.newCachedThreadPool();

    public static void timedRun(Runnable r, long timeout, TimeUnit unit) throws InterruptedException {
        // 將任務交給線程池，取得 Future 物件
        // 1. 將 Runnable 包裝成 FutureTask
        // 2. 將 futureTask 扔進佇列交給 Worker Thread 跑
        // 3. 將 futureTask 當作 Future 介面回傳給主執行緒
        Future<?> task = taskExec.submit(r);

        try {
            // 主執行緒呼叫 timed get : 最多只等 timeout 時間
            // LockSupport.park() 或 Object.wait()
            // 主執行緒進入 WAITING 或 TIMED_WAITING
            // 如何醒過來？
            // 1. 背景執行緒 呼叫 LockSupport.unpark(mainThread) 或是 notifyAll()
            // OS 收到信號，把主執行緒喚醒，狀態變為 RUNNABLE
            // 拿到結果並回傳
            // 2. 時間到，OS 喚醒主執行緒
            // 主執行緒醒來，發現任務未完成，主動拋出 TimeoutException
            // 3. 發生其他 Exception 背景執行緒主動叫醒主執行緒
            task.get(timeout, unit);
        } catch (TimeoutException e) {
            System.out.println("系統執行超時，準備取消任務");
        } catch (ExecutionException e) {
            System.out.println("任務內部發生例外");
            throw launderThrowable(e.getCause());
        } finally {
            // 關鍵神來之筆！無條件發起 cancel(true)
            // - 若已順利完成：這行沒有任何副作用 (Harmless)
            // - 若發生 TimeoutException：精準中斷正在跑該任務的背景 Worker Thread！
            task.cancel(true);
        }

    }

    /**
     * 2. 補齊：書中 (Listing 5.13) 定義的經典例外處理工具方法
     * 它的作用是把 Throwable 安全地轉型為 RuntimeException 或 Error 拋出
     */
    public static RuntimeException launderThrowable(Throwable t) {
        if (t instanceof RuntimeException)
            return (RuntimeException) t;
        else if (t instanceof Error)
            throw (Error) t;
        else
            throw new IllegalStateException("Not unchecked", t);
    }
}
