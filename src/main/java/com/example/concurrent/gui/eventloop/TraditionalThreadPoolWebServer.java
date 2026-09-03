package com.example.concurrent.gui.eventloop;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalTime;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

// 特點：寫法簡單線性。
//致命缺點：如果有 200 個人同時連進來
// 前 100 個人會把 100 條 Thread 通通佔滿睡死
// 第 101 個人必須在排隊佇列中乾
// 若是無限制開 Thread，幾千個請求就會導致記憶體崩潰。
public class TraditionalThreadPoolWebServer {
    private static final Executor exec = Executors.newFixedThreadPool(100);
    
    public static void main(String[] args) throws IOException {
        ServerSocket socket = new ServerSocket(8080);
        System.out.println("【傳統多執行緒伺服器】監聽 8080...");

        while (true) {
            Socket conn = socket.accept();
            exec.execute(() -> handleRequest(conn));
        }
    }

    private static void handleRequest(Socket conn) {
        System.out.println("[" + LocalTime.now() + " - " + Thread.currentThread().getName() + "] 收到連線");
        try (Socket s = conn) {
            // ⚠️ 執行緒在此卡住（Block）發呆 5 秒
            Thread.sleep(5000);
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("[" + LocalTime.now() + " - " + Thread.currentThread().getName() + "] 處理完成！");
    }
}
