package com.example.concurrent.futurestudy;

import java.util.concurrent.*;

// Future 介面的標準實作，同時也是一個 Runnable
// 既是 任務本體 又是 取件收據
// 能直接交給執行緒池執行
// 也能非同步結果管理
public class FutureTask<V> implements RunnableFuture<V> {

    // 紀錄任務進度
    private volatile int state;
    // 剛建立，尚未執行
    private static final int NEW = 0;
    // 正在寫入結果或例外
    private static final int COMPLETING = 1;
    // 正常執行結束
    private static final int NORMAL = 2;
    // 執行過程中拋出例外結束
    private static final int EXCEPTIONAL = 3;
    // 被呼叫 cancel(false) 取消
    private static final int CANCELLED = 4;
    // 正在發送中斷訊號給執行緒
    private static final  int INTERRUPTING = 5;
    // 被呼叫 cancel(true) 且已被中斷完成
    private static final int INTERRUPTED = 6;

    private Callable<V> callable;

    // 結果 或 Exception
    private Object outcome;

    // 方式 1：包裝一個 Callable（有回傳值、可拋出 Checked Exception）
    public FutureTask(Callable<V> callable) {
        if (callable == null) throw new NullPointerException();
        this.callable = callable;
        this.state = NEW;
    }

    // 方式 2：包裝一個 Runnable（無回傳值，完成時回傳預設的 result 物件）
    public FutureTask(Runnable runnable, V result) {
        this.callable = Executors.callable(runnable, result);
        this.state = NEW;
    }

    // 實作 Runnable：啟動任務並在內部執行 Callable.call()
    @Override
    public void run() {
//        if (state != NEW ||
//                !RUNNER.compareAndSet(this, null, Thread.currentThread()))
//            return;
//        try {
//            Callable<V> c = callable;
//            if (c != null && state == NEW) {
//                V result;
//                boolean ran;
//                try {
//                    result = c.call();
//                    ran = true;
//                } catch (Throwable ex) {
//                    result = null;
//                    ran = false;
//                    setException(ex);
//                }
//                if (ran)
//                    set(result);
//            }
//        } finally {
//            // runner must be non-null until state is settled to
//            // prevent concurrent calls to run()
//            runner = null;
//            // state must be re-read after nulling runner to prevent
//            // leaked interrupts
//            int s = state;
//            if (s >= INTERRUPTING)
//                handlePossibleCancellationInterrupt(s);
//        }
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return false;
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    @Override
    public boolean isDone() {
        return false;
    }

    @Override
    public V get() throws InterruptedException, ExecutionException {
        return null;
    }

    @Override
    public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return null;
    }

    // 核心回呼鉤子 (Hook)：當任務結束（無論成功、失敗或取消）時，底層會自動呼叫此方法
    // 在 GUI 框架（如 Swing BackgroundTask）中，常覆寫此方法切換回 EDT 更新 UI
    protected void done() { }

    // 設定成功結果（將狀態改為 NORMAL 並喚醒所有在 get() 排隊等待的 Thread）
    protected void set(V v) {

    }

    // 設定失敗例外（將狀態改為 EXCEPTIONAL 並喚醒等待的 Thread）
    protected void setException(Throwable t) { }


    // 重新設定狀態為 NEW，允許重複執行此任務（較少直接使用）
    protected boolean runAndReset() {
        return false;
    }
}
