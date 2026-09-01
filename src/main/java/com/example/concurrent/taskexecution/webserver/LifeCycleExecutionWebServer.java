package com.example.concurrent.taskexecution.webserver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.time.LocalTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;

public class LifeCycleExecutionWebServer {
    private static final int NTHREADS = 100;
    // 提供 優雅關閉的方法
    private static final ExecutorService exec = Executors.newFixedThreadPool(NTHREADS);
    private static volatile ServerSocket serverSocket;

    static void main(String[] args) throws IOException {
        // 創建一個 端點 監聽 8080
        try (ServerSocket socket = new ServerSocket(8080)) {
            serverSocket = socket;
            System.out.println("伺服器已啟動，監聽 Port 8080...");

            // 2. 當 ExecutorService 尚未關閉時，持續接收請求
            // 讓 main thread 能夠順利「執行完畢並自然結束」
            while (!exec.isShutdown()) {
                try {
                    // 主執行緒取得連線，沒有請求進入就 阻塞
                    final Socket connection = socket.accept();
                    // 將連線包成一個 task
                    Runnable task = () -> handleRequest(connection);
                    // 提交任務
                    // 若 Executor 關閉或執行緒池滿拋出 RejectedExecutionException
                    // 避免 非守護執行緒（Non-daemon Thread）存活，JVM 無法關閉
                    exec.execute(task);
                } catch (RejectedExecutionException e) {
                    if (!exec.isShutdown()) {
                        System.err.println("任務被拒絕執行，Executor 關閉");
                    } else {
                        System.err.println("任務被拒絕執行，執行緒池已滿");
                    }
                } catch (SocketException e) {
                    // 當 socket 在關閉時可能會拋出此例外，確認是否為正常關閉
                    if (exec.isShutdown()) {
                        System.out.println("ServerSocket 已關閉。");
                        break;
                    }
                }
            }
        } finally {
            stop();
        }
    }

    // 提供安全關閉伺服器的方法
    public static void stop() {
        exec.shutdown(); // 停止接收新任務

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

        try (Socket socket = connection;
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            String requestLine = in.readLine();

            // 收到 shutdown 請求
            if (requestLine != null && requestLine.contains("/shutdown")) {
                System.out.println("[" + LocalTime.now() + "] 收到關機請求，準備關閉伺服器...");
                // 呼叫 shutdown
                stop();
            } else {
                System.out.println("[" + LocalTime.now() + "] 收到連線，開始處理...");
                // 模擬這個請求需要處理 5 秒鐘（例如：查詢大型資料庫）
                Thread.sleep(5000);
                System.out.println("[" + LocalTime.now() + "] 連線處理完成！");
            }
        } catch (IOException | InterruptedException e) {
            System.err.println("處理請求時發生例外: " + e.getMessage());
        }


    }
}
