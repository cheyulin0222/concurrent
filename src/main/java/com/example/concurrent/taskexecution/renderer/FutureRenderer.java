package com.example.concurrent.taskexecution.renderer;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

// Future 版：
// 使用者點進網頁，2 秒後文字就先出來了
// 使用者可以先讀文章，背景繼續默默下載圖片
public class FutureRenderer {
    private final ExecutorService executor = Executors.newFixedThreadPool(10);

    public static final List<ImageInfo> IMAGES_INFO = List.of(
            new ImageInfo("http://example.com/image1.png"),
            new ImageInfo("http://example.com/image2.png"),
            new ImageInfo("http://example.com/image3.png")
    );

    public void renderPage() {
        // 使用 Callable，可回傳，可拋出錯誤
        // 將全部圖片下載包成一個 task
        Callable<List<ImageData>> task = () -> {
            List<ImageData> result = new ArrayList<>();
            for (ImageInfo imageInfo : IMAGES_INFO) {
                result.add(imageInfo.downloadImage());
            }
            return result;
        };

        // 主執行緒將圖片下載 委託給其他執行緒 在背景執行
        // 先回傳一個 Future 物件
        Future<List<ImageData>> future = executor.submit(task);

        // 主執行緒 渲染文字，耗時 2 秒
        renderText();

        try {
            // 主執行緒 阻塞等待圖片下載完成
            List<ImageData> imageData = future.get();

            // 依序渲染所有圖片
            // 耗時 2 * 3 秒
            for (ImageData data : imageData) {
                renderImage(data);
            }
        } catch (ExecutionException e) {
            // 任務異常會包裝為 ExecutionException 拋出
            throw new RuntimeException(e.getCause());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            // 收到中斷，任務取消
            // true 向進行中任務的 thread 發送 Thread.interrupt()，中斷它
            // false 允許他繼續跑完，但標記此 Future 為已取消
            future.cancel(true);
        }
    }

    private void renderText() {
        log("開始渲染 HTML 文字內容...");
        try {
            // 模擬 CPU 運算與排版，耗時 2 秒
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        log("HTML 文字內容渲染完成！");
    }

    private void renderImage(ImageData data) {
        log("開始渲染 圖片...");
        try {
            // 模擬 CPU 渲染圖片，耗時 2 秒
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        log("將圖片繪製到畫面上： " + data.data());
    }

    private static void log(String msg) {
        System.out.println("[" + LocalTime.now() + "] " + msg);
    }

    // 圖片檔案
    record ImageData(String data) {}

    // 圖片URL
    static class ImageInfo {
        private final String url;

        public ImageInfo(String url) {
            this.url = url;
        }

        public ImageData downloadImage() {
            log("開始下載圖片： " + url);
            try {
                // 模擬每張圖片下載需要 2 秒
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            log("圖片下載完成： " + url);
            return new ImageData("ImageDataFor[" + url + "]");
        }
    }

    static void main() {
        FutureRenderer render = new FutureRenderer();
        long startTime = System.currentTimeMillis();

        render.renderPage();

        long endTime = System.currentTimeMillis();

        System.out.println("\n總共耗時: " + (endTime - startTime) + " 毫秒");
    }

}
