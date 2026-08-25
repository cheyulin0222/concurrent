package com.example.concurrent.taskexecution;

import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.*;

public class TravelPortalExample {

    private final ExecutorService exec = Executors.newFixedThreadPool(5);

    public List<TravelQuote> getRankedTravelQuotes(Set<TravelCompany> companies, long timeout, TimeUnit unit) {

    }

    public List<TravelQuote> getRankedTravelQuotes(Set<TravelCompany> companies, long timeout, TimeUnit unit) throws InterruptedException {
        List<QuoteTask> tasks = new ArrayList<>();

        for (TravelCompany company : companies) {
            tasks.add(new QuoteTask(company));
        }

        log(">>> 開始向所有航空公司發起平行詢價，總時間預算: " + timeout + " " + unit + " <<<");

        // invokeAll 會阻塞等待，直到：
        // 1. 所有任務完成 OR 2. 時間預算 (2秒) 用完
        // 2秒一到，還沒完成的任務會在內部被自動 cancel(true)！
        // futures 的順序 和 tasks 的順序是一樣的
        List<Future<TravelQuote>> futures = exec.invokeAll(tasks, timeout, unit);

        List<TravelQuote> quotes = new ArrayList<>(tasks.size());
        Iterator<QuoteTask> taskIter = tasks.iterator();

        for (Future<TravelQuote> f : futures) {
            QuoteTask task = taskIter.next();
            try {
                quotes.add(f.get());
            } catch (ExecutionException e) {
                quotes.add(task.getFailureQuote(e.getCause()));
            } catch (CancellationException e) {
                // 關鍵！只要超過 2 秒被 invokeAll 自動取消的任務，呼叫 f.get() 就會拋出此 Exception
                quotes.add(task.getTimeoutQuote());
            }
        }

        quotes.sort(Comparator.comparingDouble(TravelQuote::getPrice));
        return quotes;

    }

    public void stop() {
        exec.shutdown();
    }

    private static void log(String msg) {
        System.out.println("[" + LocalTime.now() + "] [" + Thread.currentThread().getName() + "] " + msg);
    }

    public static void main(String[] args) throws InterruptedException {
        TravelPortalExample portal = new TravelPortalExample();

        // 準備 3 家航空公司：
        // 1. 星宇航空：0.5 秒回傳 (快)
        // 2. 華航：1.5 秒回傳 (中)
        // 3. 長榮航空：3.5 秒回傳 (慢，必然超過 2 秒預算)
        Set<TravelCompany> companies = new LinkedHashSet<>();
        companies.add(new TravelCompany("興宇航空", 0, 15000)); // 0.5s 改為 0 秒示範快
        companies.add(new TravelCompany("中華航空", 1, 13500)); // 1 秒
        companies.add(new TravelCompany("長榮航空", 3, 12000)); // 3 秒 (會逾時)

        // 設定時間預算 2 秒
        List<TravelQuote> rankedQuotes = portal.getRankedTravelQuotes(companies, 2, TimeUnit.SECONDS);

        System.out.println("\n========== 比價結果 (從便宜到貴) ==========");
        for (TravelQuote quote : rankedQuotes) {
            System.out.println(quote);
        }
        System.out.println("===========================================\n");

        portal.stop();
    }


}
