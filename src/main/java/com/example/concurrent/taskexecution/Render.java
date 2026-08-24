package com.example.concurrent.taskexecution;

import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;

public class Render {
    private final ExecutorService executor;

    Render(ExecutorService executor) {
        this.executor = executor;
    }

    public static void main(String[] args) {
        Render renderer = new Render(Executors.newFixedThreadPool(10));
        long startTime = System.currentTimeMillis();

        renderer.renderPage("<html><body><h1>Hello JCiP</h1></body></html>");

        long endTime = System.currentTimeMillis();

        System.out.println("\n總共耗時: " + (endTime - startTime) + " 毫秒");
    }

    public void renderPage(CharSequence source) {
        List<ImageInfo> imageInfos = scanForImageInfo(source);

        CompletionService<ImageData> completionService = new ExecutorCompletionService<>(executor);

        // 1. 為「每一張圖片」提交一個獨立的下載任務 (平行併發下載)
        for (final ImageInfo imageInfo : imageInfos) {
            Callable<ImageData> task = imageInfo::downloadImage;
            // 開始執行
            completionService.submit(task);
        }

        // 2. 主線程同時進行 HTML 文字渲染
        renderText(source);

        try {
            // 3. 關鍵！迴圈 N 次，誰先下載好就先 take() 出來繪製
            for (int t = 0, n = imageInfos.size(); t < n; t++) {
                // 向 CompletionService 內部的「已完成佇列（Completion Queue）」要一個已經執行完畢的 Future。
                // 阻塞等待「最快完成」的那一個任務
                Future<ImageData> f = completionService.take();
                ImageData imageData = f.get(); // 不會阻塞
                // 渲染
                renderImage(imageData);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
            throw new RuntimeException(e.getCause());
        } finally {
            executor.shutdown();
        }
    }

    private List<ImageInfo> scanForImageInfo(CharSequence source) {
        // 模擬從 HTML 中解析出 3 張圖片的 URL
        return Arrays.asList(
                new ImageInfo("http://example.com/image1.png"),
                new ImageInfo("http://example.com/image2.png"),
                new ImageInfo("http://example.com/image3.png")
        );
    }

    private void renderText(CharSequence source) {
        log("渲染 HTML 文字內容： " + source);
    }

    private void renderImage(ImageData data) {
        log("將圖片繪製到畫面上： " + data.getData());
    }

    private static void log(String msg) {
        System.out.println("[" + LocalTime.now() + "] " + msg);
    }


}
