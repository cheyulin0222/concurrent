package com.example.concurrent.taskexecution.timebudget;

import com.example.concurrent.component.QuoteTask;
import com.example.concurrent.component.TravelCompany;
import com.example.concurrent.component.TravelQuote;

import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.*;

public class TravelPortalRenderer {
    private final ExecutorService exec = Executors.newFixedThreadPool(5);

    // 將向三個航空公司詢價任務包成 Callable 假定了 網路延遲時間 與 價錢
    public static final List<QuoteTask> TASKS = List.of(
            new QuoteTask(new TravelCompany("星宇航空", 0, 15000)),
            new QuoteTask(new TravelCompany("中華航空", 1, 13500)),
            new QuoteTask(new TravelCompany("長榮航空", 3, 12000))
    );

    static void main() throws InterruptedException {
        TravelPortalRenderer portal = new TravelPortalRenderer();
        // 向航空公司 Server 詢價
        // 設定時間預算 2 秒
        List<TravelQuote> rankedQuotes = portal.getRankedTravelQuotes(2, TimeUnit.SECONDS);

        System.out.println("\n========== 比價結果 (從便宜到貴) ==========");
        for (TravelQuote quote : rankedQuotes) {
            System.out.println(quote);
        }
        System.out.println("===========================================\n");

    }

    public List<TravelQuote> getRankedTravelQuotes(long timeout, TimeUnit unit) throws InterruptedException {
        log(">>> 開始向所有航空公司發起平行詢價，總時間預算: " + timeout + " " + unit + " <<<");

        // invokeAll 會阻塞等待，直到：
        // 1. 所有任務完成 OR 2. 時間預算 (2秒) 用完
        // 2秒 一到，還沒完成的任務會在內部被自動 cancel(true)
        List<Future<TravelQuote>> futures = exec.invokeAll(TASKS, timeout, unit);

        // 取得任務的 iterator，用來之後錯誤處理用
        Iterator<QuoteTask> taskIter = TASKS.iterator();
        List<TravelQuote> quotes = new ArrayList<>(TASKS.size());

        for (Future<TravelQuote> future : futures) {
            // futures 的順序 和 tasks 的順序是一樣的，所以可以拿來定位回傳的 future
            QuoteTask task = taskIter.next();
            try {
                // 取得 各別航空公司報價
                // Timeout 的會拋出 CancellationException
                quotes.add(future.get());
            } catch (ExecutionException e) {
                // 取得 航空公司 default 的 ExecutionException 回傳
                quotes.add(task.getFailureQuote(e.getCause()));
            } catch (CancellationException e) {
                // 超過 2 秒被 invokeAll 自動取消的任務，呼叫 future.get() 就會拋出此 Exception
                // 取得 航空公司 default 的 timeout 回傳
                quotes.add(task.getTimeoutQuote());
            }
        }

        quotes.sort(Comparator.comparingDouble(TravelQuote::getPrice));
        return quotes;
    }

    private static void log(String msg) {
        System.out.println("[" + LocalTime.now() + "] [" + Thread.currentThread().getName() + "] " + msg);
    }




}
