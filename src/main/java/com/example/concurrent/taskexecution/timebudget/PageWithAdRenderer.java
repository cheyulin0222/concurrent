package com.example.concurrent.taskexecution.timebudget;

import com.example.concurrent.component.Ad;
import com.example.concurrent.component.FetchAdTask;
import com.example.concurrent.component.Page;

import java.time.LocalTime;
import java.util.concurrent.*;

// Time Budgeting
// 服務降級機制 (Fallback / Graceful Degradation)
// 資源清理 (Resource Clean-up)

// 缺點
// 程式碼複雜度較高 :
// 須自己維護 endNanos、timeLeft、TimeoutException、呼叫 cancel(true)

// 單一任務控時，難以推廣到多任務
// 如果網頁需要同時抓取廣告、推薦系統、使用者頭像、氣象 4 個服務，自己用 f.get(timeLeft) 寫迴圈扣除剩餘時間會變得非常痛苦

// 非真正的一部響應
// 主線程在 f.get(timeLeft) 期間依然是阻塞（Blocking）的。如果這 1 秒內有其他事可以做，主線程無法邊做別的事邊等（現代 Java 通常會改用 CompletableFuture 或 WebFlux 來做到徹底的非阻塞 Non-blocking）。

public class PageWithAdRenderer {

    private final ExecutorService exec = Executors.newFixedThreadPool(2);
    // 預設廣告備案
    private static final Ad DEFAULT_AD = new Ad("【預設廣告】歡迎光臨我們的網站！");
    // 設定時間預算 (Time Budget)：例如只給廣告 2 秒鐘
    private static final long TIME_BUDGET_NS = TimeUnit.SECONDS.toNanos(2);

    public Page renderPageWithAd(int delaySeconds) throws InterruptedException {

        // 算出結束時間
        long endNanos = System.nanoTime() + TIME_BUDGET_NS;

        // 主執行緒 提交任務，預計耗時 delaySeconds
        Future<Ad> future = exec.submit(new FetchAdTask(delaySeconds));

        // 主執行緒 渲染頁面，耗時 1 秒
        Page page = renderPageBody();

        Ad ad;

        try {
            // 計算剩餘的時間預算 (結束時間 - 當下時間)
            long timeLeft = endNanos - System.nanoTime();
            log("主體渲染完成，剩餘廣告時間預算: " + TimeUnit.NANOSECONDS.toMillis(timeLeft) + " ms");

            // 主執行緒 使用帶有逾時設定的 future.get(...) 阻塞等待廣告結果
            // 超過時間拋出 TimeoutException
            ad = future.get(timeLeft, TimeUnit.NANOSECONDS);
        } catch (ExecutionException e) {
            log("抓取廣告發生異常，改用預設廣告");
            ad = DEFAULT_AD;
        } catch (TimeoutException e) {
            log("⏰ 廣告抓取逾時！立刻取消任務並改用預設廣告");
            ad = DEFAULT_AD;
            // Timeout 後主動取消任務
            future.cancel(true);
        }

        page.setAd(ad);
        return page;
    }

    public void stop() {
        exec.shutdown();
    }

    // 模擬主體繪製 (耗時 1 秒)
    private Page renderPageBody() {
        log("開始渲染網頁主體內容...");
        try {
            TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        log("網頁主體內容渲染完成！");
        return new Page("<html><body><h1>歡迎來到新聞首頁</h1></body></html>");
    }

    private static void log(String msg) {
        System.out.println("[" + LocalTime.now() + "] [" + Thread.currentThread().getName() + "] " + msg);
    }


    static void main() throws InterruptedException {
        PageWithAdRenderer renderer = new PageWithAdRenderer();

        // 廣告服務只花 0.5 秒回應，主執行緒渲染完主體後，f.get() 瞬間拿到結果，成功顯示「【精密投放廣告】」。
        System.out.println("=== 測試 1：廣告在 0.5 秒內回應 (正常顯示遠端廣告) ===");
        Page page1 = renderer.renderPageWithAd(0);
        page1.display();

        // 廣告服務需要 3 秒，主執行緒等完剩餘的 1 秒後觸發 TimeoutException：
        System.out.println("=== 測試 2：廣告回應需要 3 秒 (超過 2 秒預算，觸發 Timeout 改用預設廣告) ===");
        Page page2 = renderer.renderPageWithAd(3);
        page2.display();

        renderer.stop();
    }

}
