package com.example.concurrent.gui;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.*;

public abstract class BackgroundTask<V> implements Runnable, Future<V> {
    // =========================================================================
    // Listing 9.7: BackgroundTask 抽象框架
    // =========================================================================

    private final FutureTask<V> computation = new Computation();

    private class Computation extends FutureTask<V> {

        public Computation() {
            super(BackgroundTask.this::compute);
        }

        @Override
        protected final void done() {
            GuiExecutor.instance().execute(() -> {
                V value = null;
                Throwable thrown = null;
                boolean cancelled = false;

                try {
                    value = get();
                } catch (ExecutionException e) {
                    thrown = e.getCause();
                } catch (CancellationException e) {
                    // 忽略中斷
                } catch (InterruptedException e) {
                    // 忽略中斷
                } finally {
                    // 保證在 EDT (GUI 執行緒) 內呼叫 onCompletion
                    onCompletion(value, thrown, cancelled);
                }

            });
        }
    }

    // 背景任務呼叫 setProgress，會自動轉發到 EDT 觸發 onProgress
    protected void setProgress(final int current, final int max) {
        GuiExecutor.instance().execute(() -> onProgress(current, max));
    }

    // 必須由子類別實作：在【背景執行緒】中跑耗時運算
    protected abstract V compute() throws Exception;

    // 可選覆寫：在【EDT 執行緒】中處理完成、錯誤或取消更新
    protected void onCompletion(V result, Throwable exception, boolean cancelled) {}
    // 可選覆寫：在【EDT 執行緒】中更新進度條或 UI 提示
    protected void onProgress(int current, int max) {}

    @Override
    public void run() {
        computation.run();
    }

    // Future 介面轉發
    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return computation.cancel(mayInterruptIfRunning);
    }

    @Override
    public boolean isCancelled() {
        return computation.isCancelled();
    }

    @Override
    public boolean isDone() {
        return computation.isDone();
    }

    @Override
    public V get() throws ExecutionException, InterruptedException {
        return computation.get();
    }

    @Override
    public V get(long timeout, TimeUnit unit) throws ExecutionException, InterruptedException, TimeoutException {
        return computation.get(timeout, unit);
    }





}
