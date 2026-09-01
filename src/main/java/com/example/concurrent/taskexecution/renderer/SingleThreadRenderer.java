package com.example.concurrent.taskexecution.renderer;


import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

// 單執行緒版：
// 使用者點進網頁，畫面完全凍結白布
public class SingleThreadRenderer {

    public static final List<ImageInfo> IMAGES_INFO = List.of(
            new ImageInfo("http://example.com/image1.png"),
            new ImageInfo("http://example.com/image2.png"),
            new ImageInfo("http://example.com/image3.png")
    );

    public void renderPage() {
        // 依序下載所有圖片
        // 耗時 2 * 3 秒
        List<ImageData> imageData = new ArrayList<>();
        for (ImageInfo imageInfo : IMAGES_INFO)
            imageData.add(imageInfo.downloadImage());

        // 渲染文字，耗時 2 秒
        renderText();

        // 依序渲染所有圖片
        // 耗時 2 * 3 秒
        for (ImageData data : imageData)
            renderImage(data);
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

    private static void log(String msg) {
        System.out.println("[" + LocalTime.now() + "] " + msg);
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

    // 圖片檔案
    record ImageData(String data) {}

    static void main() {
        SingleThreadRenderer renderer = new SingleThreadRenderer();
        long startTime = System.currentTimeMillis();

        renderer.renderPage();

        long endTime = System.currentTimeMillis();

        System.out.println("\n總共耗時: " + (endTime - startTime) + " 毫秒");
    }


}
