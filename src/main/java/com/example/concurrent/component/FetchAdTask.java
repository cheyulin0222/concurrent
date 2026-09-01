package com.example.concurrent.component;

import java.time.LocalTime;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

public class FetchAdTask implements Callable<Ad> {
    private final int delaySeconds;

    public FetchAdTask(int delaySeconds) {
        this.delaySeconds = delaySeconds;
    }

    @Override
    public Ad call() throws Exception {
        log("開始向遠端 Ad Server 請求廣告...");
        // 模擬網路延遲
        TimeUnit.SECONDS.sleep(delaySeconds);
        log("成功抓取到遠端客製化廣告！");
        return new Ad("【精密投放廣告】85 折折扣碼：SAVE15");
    }

    private static void log(String msg) {
        System.out.println("[" + LocalTime.now() + "] [" + Thread.currentThread().getName() + "] " + msg);
    }
}
