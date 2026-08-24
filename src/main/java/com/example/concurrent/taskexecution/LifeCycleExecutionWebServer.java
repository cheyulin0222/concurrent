package com.example.concurrent.taskexecution;

import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;

public class LifeCycleExecutionWebServer {
    private static final int NTHREADS = 100;
    private static final ExecutorService exec = Executors.newFixedThreadPool(NTHREADS);
    private static volatile ServerSocket serverSocket;

    public static void main(String[] args) throws IOException {
        try (ServerSocket socket = new ServerSocket(8080)) {
            serverSocket = socket;
            System.out.println("伺服器已啟動，監聽 Port 8080...");

            // 2. 當 ExecutorService 尚未關閉時，持續接收請求
            // 讓 main thread 能夠順利「執行完畢並自然結束」
            while (!exec.isShutdown()) {
                try {
                    final Socket connection = socket.accept();
                    Runnable task = () -> handleRequest(connection);
                    // 提交任務
                    exec.execute(task);
                } catch (SocketException e) {
                    // 當 socket 在關閉時可能會拋出此例外，確認是否為正常關閉
                    if (exec.isShutdown()) {
                        System.out.println("ServerSocket 已關閉。");
                        break;
                    }
                } catch (RejectedExecutionException e) {
                    if (!exec.isShutdown()) {
                        System.err.println("任務被拒絕執行: " + e.getMessage());
                    }
                }
            }
        } finally {
            stop();
        }
    }

    // 提供安全關閉伺服器的方法
    public static void stop() {
        if (!exec.isShutdown()) {
            System.out.println("正在關閉 ExecutorService...");
            exec.shutdown(); // 停止接收新任務
        }

        // 關閉 ServerSocket 以解開 socket.accept() 的阻塞
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close(); // 觸發 accept() 拋出 SocketException
            } catch (IOException e) {
                // 忽略關閉時的例外
            }
        }
    }

    private static void handleRequest(Socket connection) {
        System.out.println("[" + LocalTime.now() + "] 收到連線，開始處理...");

        try (Socket socket = connection;
             OutputStream out = socket.getOutputStream()) {

            // 模擬這個請求需要處理 5 秒鐘（例如：查詢大型資料庫）
            Thread.sleep(5000);

            String body = "<h1>Hello World from WebServer!</h1>";
            byte[] bodyBytes = body.getBytes(StandardCharsets.UTF_8);

            // 2. 組成 HTTP Header（Content-Length 精確計算 bodyBytes 的長度）
            String header = "HTTP/1.1 200 OK\r\n" +
                    "Content-Type: text/html; charset=UTF-8\r\n" +
                    "Content-Length: " + bodyBytes.length + "\r\n" +
                    "\r\n"; // 兩個 \r\n 代表 Header 結束

            // 3. 先發送 Header，再發送 Body
            out.write(header.getBytes(StandardCharsets.UTF_8));
            out.write(bodyBytes);
            out.flush();

        } catch (IOException | InterruptedException e) {
            System.err.println("處理請求時發生例外: " + e.getMessage());
        }

        System.out.println("[" + LocalTime.now() + "] 連線處理完成！");
        System.out.println("----------------------------------------");
    }
}
