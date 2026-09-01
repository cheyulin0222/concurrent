package com.example.concurrent.taskexecution.webserver;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalTime;

public class SingleThreadWebserver {
    
    static void main() throws IOException {
        // 創建一個 端點 監聽 8080
        ServerSocket socket = new ServerSocket(8080);
        System.out.println("伺服器已啟動，監聽 Port 8080...");

        while (true) {
            // 主執行緒取得連線，沒有請求進入就 阻塞
            Socket connection = socket.accept();
            // 主執行緒自已處理連線
            handleRequest(connection);
        }
    }

    private static void handleRequest(Socket connection) {
        try (Socket socket = connection) {
            System.out.println("[" + LocalTime.now() + "] 收到連線，開始處理...");
            // 模擬請求需要處理 5 秒鐘（例如：查詢大型資料庫）
            Thread.sleep(5000);

            System.out.println("[" + LocalTime.now() + "] 連線處理完成！");
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }

    }
}
