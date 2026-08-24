package com.example.concurrent.taskexecution;

import java.time.LocalTime;
import java.util.concurrent.*;

public class TimeAdRenderer {

    // 建立線程池處理背景廣告抓取
    private final ExecutorService exec = Executors.newFixedThreadPool(2);

    // 預設廣告備案 (當超時或出錯時使用)
    private static final Ad DEFAULT_AD = new Ad("【預設廣告】歡迎光臨我們的網站！");

    // 設定時間預算 (Time Budget)：例如只給廣告 2 秒鐘
    private static final long TIME_BUDGET_NS = TimeUnit.SECONDS.toNanos(2);

    public Page renderPageWithAd(int delaySeconds) {
        // 計算截止時間點 (Deadline)
        long endNanos = System.nanoTime() + TIME_BUDGET_NS;

        Future<Ad> f = exec.submit(new FetchAdTask(delaySeconds));

        Page page = renderPageBody();

        Ad ad;

        try {
            // 3. 計算剩餘的時間預算 (Time Budget)
            long timeLeft = endNanos - System.nanoTime();

            log("主體渲染完成，剩餘廣告時間預算: " + TimeUnit.NANOSECONDS.toMillis(timeLeft) + " ms");

            // 4. 使用帶有逾時設定的 f.get(...) 等待廣告結果
            ad = f.get(timeLeft, TimeUnit.NANOSECONDS);
        } catch (ExecutionException e) {
            log("抓取廣告發生異常，改用預設廣告");
            ad = DEFAULT_AD;
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (TimeoutException e) {
            log("⏰ 廣告抓取逾時！立刻取消任務並改用預設廣告");
            ad = DEFAULT_AD;
            f.cancel(true);
        }

        page.setAd(ad);
        return page;
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

}
