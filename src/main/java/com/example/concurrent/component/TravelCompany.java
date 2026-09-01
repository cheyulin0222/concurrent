package com.example.concurrent.component;

import java.time.LocalTime;
import java.util.concurrent.TimeUnit;

// 航空公司
public class TravelCompany {
    private final String name;
    private final int responseDelaySeconds;
    private final double price;

    public TravelCompany(String name, int responseDelaySeconds, double price) {
        this.name = name;
        this.responseDelaySeconds = responseDelaySeconds;
        this.price = price;
    }

    public TravelQuote solicitQuote() throws InterruptedException {
        log("向 [" + name + "] 發送詢價請求...");

        // 模擬網路 API 呼叫耗時
        TimeUnit.SECONDS.sleep(responseDelaySeconds);

        log("[" + name + "] 成功回傳報價！");
        return new TravelQuote(name, price);
    }

    public String getName() {
        return name;
    }

    private static void log(String msg) {
        System.out.println("[" + LocalTime.now() + "] [" + Thread.currentThread().getName() + "] " + msg);
    }
}
