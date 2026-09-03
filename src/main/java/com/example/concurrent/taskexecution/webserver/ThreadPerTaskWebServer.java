package com.example.concurrent.taskexecution.webserver;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalTime;
import java.util.concurrent.Executor;

public class ThreadPerTaskWebServer implements Executor {

    @Override
    public void execute(Runnable command) {
        new Thread(command).start();
    }

    static void main() throws IOException {
        // 創建一個 端點 監聽 8080
        ServerSocket socket = new ServerSocket(8080);
        System.out.println("伺服器已啟動，監聽 Port 8080...");

        while (true) {
            // 主執行緒 取得連線
            final Socket connection = socket.accept();
            // 將連線包成一個 task
            Runnable task = () -> handleRequest(connection);
            // 呼叫一個新的 執行緒 去執行這個 task
            // (生產者/消費者) 模式 中的 任務提交 與 任務執行 解耦
            new Thread(task).start();
        }
    }

    public static void handleRequest(Socket connection) {
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
