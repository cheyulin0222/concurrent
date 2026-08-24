package com.example.concurrent.taskexecution;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;

// Future 版：
// 使用者點進網頁，1 秒後文字就先出來了！
// 使用者可以先讀文章，背景繼續默默下載圖片，4.5 秒 後圖片才補上。
public class FutureRenderer {
    private final ExecutorService executor = Executors.newFixedThreadPool(10);
    
    public static void main(String[] args) {
        FutureRenderer render = new FutureRenderer();
        long startTime = System.currentTimeMillis();

        render.renderPage("<html><body><h1>Hello JCiP Future</h1></body></html>");

        long endTime = System.currentTimeMillis();

        System.out.println("\n總共耗時: " + (endTime - startTime) + " 毫秒");
    }

    public void renderPage(CharSequence source) {
        List<ImageInfo> imageInfos = scanForImageInfo(source);

        // 使用 Callable，可回傳，可拋出錯誤
        Callable<List<ImageData>> task = () -> {
            List<ImageData> result = new ArrayList<>();
            for (ImageInfo imageInfo : imageInfos) {
                ImageData imageData = imageInfo.downloadImage();
                result.add(imageData);
            }
            return result;
        };

        Future<List<ImageData>> future = executor.submit(task);

        renderText(source);

        try {
            // 任務未完成會阻塞等待
            List<ImageData> imageData = future.get();
            for (ImageData data : imageData) {
                renderImage(data);
            }
        } catch (ExecutionException e) {
            // 任務異常會包裝為 ExecutionException 拋出
            throw new RuntimeException(e.getCause());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            // 收到中斷，任務取消
            // true 向進行中任務的 thread 發送 Thread.interrupt()，中斷他
            // false 允許他繼續跑完，但標記此 Future 為已取消 (之後呼叫 get() 會拋例外）
            future.cancel(true);
        } finally {
            executor.shutdown();
        }
    }

    private void renderText(CharSequence source) {
        LOG.log("渲染 HTML 文字內容： " + source);
    }

    private List<ImageInfo> scanForImageInfo(CharSequence source) {
        // 模擬從 HTML 中解析出 3 張圖片的 URL
        return Arrays.asList(
                new ImageInfo("http://example.com/image1.png"),
                new ImageInfo("http://example.com/image2.png"),
                new ImageInfo("http://example.com/image3.png")
        );
    }

    private void renderImage(ImageData data) {
        LOG.log("將圖片繪製到畫面上： " + data.getData());
    }

}
