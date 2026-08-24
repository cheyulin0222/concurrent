package com.example.concurrent.taskexecution;

import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalTime;
import java.util.concurrent.Executor;

public class ThreadPerTaskWebServer implements Executor {

    @Override
    public void execute(Runnable command) {
        new Thread(command).start();
    }

    public static void main(String[] args) throws IOException {
        ServerSocket socket = new ServerSocket(8080);
        System.out.println("伺服器已啟動，監聽 Port 8080...");
        while (true) {
            final Socket connection = socket.accept();

            Runnable task = () -> {
                System.out.println("[" + LocalTime.now() + "] 收到連線，開始處理...");
                handleRequest(connection);
                System.out.println("[" + LocalTime.now() + "] 連線處理完成！");
                System.out.println("----------------------------------------");
            };

            new Thread(task).start();
        }
    }

    private static void handleRequest(Socket connection) {
        try (Socket socket = connection;
             OutputStream out = socket.getOutputStream()) {

            // 模擬這個請求需要處理 5 秒鐘（例如：查詢大型資料庫）
            Thread.sleep(5000);

            String body = "<h1>Hello World from WebServer!</h1>";
            byte[] bodyBytes = body.getBytes("UTF-8");

            // 2. 組成 HTTP Header（Content-Length 精確計算 bodyBytes 的長度）
            String header = "HTTP/1.1 200 OK\r\n" +
                    "Content-Type: text/html; charset=UTF-8\r\n" +
                    "Content-Length: " + bodyBytes.length + "\r\n" +
                    "\r\n"; // 兩個 \r\n 代表 Header 結束

            // 3. 先發送 Header，再發送 Body
            out.write(header.getBytes("UTF-8"));
            out.write(bodyBytes);
            out.flush();

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }


}
