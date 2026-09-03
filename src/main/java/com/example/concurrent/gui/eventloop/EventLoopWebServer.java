package com.example.concurrent.gui.eventloop;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.time.LocalTime;
import java.util.Iterator;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class EventLoopWebServer {

    // 模擬底層系統定時器（在真實 Netty/Node.js 中由底層 Event Loop 定時輪詢提供）
    private static final ScheduledExecutorService timer = Executors.newSingleThreadScheduledExecutor();

    public static void main(String[] args) throws IOException {
        // 1. 開啟非阻塞 ServerSocketChannel
        ServerSocketChannel channel = ServerSocketChannel.open();
        channel.bind(new InetSocketAddress(8080));
        channel.configureBlocking(false); // 關鍵：設定為非阻塞

        // 2. 建立 Selector（相當於 Linux 的 epoll，也是 Event Loop 的核心信箱）
        Selector selector = Selector.open();
        channel.register(selector, SelectionKey.OP_ACCEPT);

        System.out.println("【Event Loop 單執行緒伺服器】監聽 8080...");


        // 3. 唯一的事件迴圈（Event Loop）
        while (true) {
            // 阻塞直到有任何網路事件發生（完全不消耗多餘 CPU）
            selector.select();

            Iterator<SelectionKey> keyIterator = selector.selectedKeys().iterator();
            while (keyIterator.hasNext()) {
                SelectionKey key = keyIterator.next();
                keyIterator.remove();

                if (key.isAcceptable()) {
                    // 有新的客戶端連線進來
                    handleAccept(channel);
                }
            }
        }
    }

    private static void handleAccept(ServerSocketChannel channel) throws IOException {
        SocketChannel clientChannel = channel.accept();
        if (clientChannel == null) return;

        clientChannel.configureBlocking(false);
        System.out.println("[" + LocalTime.now() + " - " + Thread.currentThread().getName() + "] 收到連線！");

        // ⚠️ 關鍵區別：主執行緒絕不睡覺！
        // 登記「5 秒後執行完成回呼」，主執行緒瞬間抽身去接下一個人的請求
        timer.schedule(() -> {
            // 5 秒時間到，觸發回呼處理
           try {
               System.out.println("[" + LocalTime.now() + " - " + Thread.currentThread().getName() + "] 5秒非同步等待完成，關閉連線！");
               clientChannel.close();
           } catch (IOException e) {
               e.printStackTrace();
           }
        }, 5, TimeUnit.SECONDS);


        System.out.println("[" + LocalTime.now() + " - " + Thread.currentThread().getName() + "] 主執行緒已登記任務，立刻準備接下一單！");
    }

}
