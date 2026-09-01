package com.example.concurrent.taskexecution.renderer;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

// CompletionService 版
// 渲染 和 下載 倂行處理
public class CompletionServiceRenderer {
    private final ExecutorService executor;

    public static final List<ImageInfo> IMAGES_INFO = List.of(
            new ImageInfo("http://example.com/image1.png"),
            new ImageInfo("http://example.com/image2.png"),
            new ImageInfo("http://example.com/image3.png")
    );

    CompletionServiceRenderer(ExecutorService executor) {
        this.executor = executor;
    }

    public void renderPage() {
        // 建立一個 CompletionService
        // 內部自己維護一個 BlockingQueue<Future<ImageData>>
        CompletionService<ImageData> completionService = new ExecutorCompletionService<>(executor);

        // 為「每一張圖片」提交一個獨立的下載任務 (平行併發下載)
        List<Future<ImageData>> futures = new ArrayList<>(); // 為了後面的 cancel
        for (final ImageInfo imageInfo : IMAGES_INFO) {
            Callable<ImageData> task = imageInfo::downloadImage;
            // 主執行緒將 每一張圖片下載 委託給其他執行緒
            futures.add(completionService.submit(task));
        }

        // 主執行緒 渲染文字，耗時 2 秒
        renderText();

        try {
            // 迴圈 N 次，誰先下載好就先 take() 出來繪製
            for (int t = 0, n = IMAGES_INFO.size(); t < n; t++) {
                // 向 CompletionService 內部的「已完成佇列（Completion Queue）」要一個已經執行完畢的 Future。
                // 主執行緒 阻塞等待「最快完成」的那一個任務
                Future<ImageData> future = completionService.take();
                ImageData imageData = future.get(); // 不會阻塞
                // 主執行緒渲染圖片 2 * 3 秒
                renderImage(imageData);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            // 任務取消
            cancelAll(futures);
        } catch (ExecutionException e) {
            throw new RuntimeException(e.getCause());
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

    private void cancelAll(List<Future<ImageData>> futures) {
        for (Future<ImageData> future : futures) {
            future.cancel(true); // mayInterruptIfRunning = true
        }
    }

    static void main() {
        CompletionServiceRenderer renderer = new CompletionServiceRenderer(Executors.newFixedThreadPool(10));
        long startTime = System.currentTimeMillis();

        renderer.renderPage();

        long endTime = System.currentTimeMillis();

        System.out.println("\n總共耗時: " + (endTime - startTime) + " 毫秒");
    }
}
