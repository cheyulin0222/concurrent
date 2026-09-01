package com.example.concurrent.taskexecution.webserver;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalTime;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class TaskExecutionWebServer {
    private static final int NTHREADS = 100;
    // 策略模式
    // 負責 執行策略
    // 使用 固定執行緒池解決 無限制開 Thread 導致 OOM 崩潰
    private static final Executor exec = Executors.newFixedThreadPool(NTHREADS);
//    private static final Executor exec = Executors.newSingleThreadExecutor();
//    private static final Executor exec = Executors.newThreadPerTaskExecutor();


    static void main() throws IOException {
        // 創建一個 端點 監聽 8080
        ServerSocket socket = new ServerSocket(8080);
        System.out.println("伺服器已啟動，監聽 Port 8080...");

        while (true) {
            // 主執行緒取得連線
            final Socket connection = socket.accept();
            // 將連線包成一個 task
            Runnable task = () -> handleRequest(connection);
            // 主執行緒 只要專心提交任務
            exec.execute(task);
        }
    }

    private static void handleRequest(Socket connection) {
        System.out.println("[" + LocalTime.now() + "] 收到連線，開始處理...");

        try (Socket socket = connection) {

            // 模擬這個請求需要處理 5 秒鐘（例如：查詢大型資料庫）
            Thread.sleep(5000);

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("[" + LocalTime.now() + "] 連線處理完成！");
        System.out.println("----------------------------------------");
    }
}
