package com.example.concurrent.taskexecution;


import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

// 單執行緒版：
// 使用者點進網頁，畫面完全凍結白布
// 要死等 5.5 秒 後，文字跟圖片才同時跳出來。
public class SingleThreadRenderer {

    public static void main(String[] args) {
        SingleThreadRenderer renderer = new SingleThreadRenderer();
        long startTime = System.currentTimeMillis();

        renderer.renderPage("<html><body><h1>Hello JCiP</h1></body></html>");

        long endTime = System.currentTimeMillis();

        System.out.println("\n總共耗時: " + (endTime - startTime) + " 毫秒");
    }

    public void renderPage(CharSequence source) {
        // 步驟 1: 渲染文字
        renderText(source);

        // 步驟 2: 串行下載所有圖片
        List<ImageData> imageData = new ArrayList<>();
        for (ImageInfo imageInfo : scanForImageInfo(source))
            imageData.add(imageInfo.downloadImage());

        // 步驟 3: 渲染所有圖片
        for (ImageData data : imageData)
            renderImage(data);
    }

    private void renderText(CharSequence source) {
        log("渲染 HTML 文字內容： " + source);
    }

    private List<ImageInfo> scanForImageInfo(CharSequence source) {
        // 模擬從 HTML 中解析出 3 張圖片的 URL
        return Arrays.asList(
                new ImageInfo("http://example.com/image1.png"),
                new ImageInfo("http://example.com/image2.png"),
                new ImageInfo("http://example.com/image3.png")
        );
    }

    private static void log(String msg) {
        System.out.println("[" + LocalTime.now() + "] " + msg);
    }

    private void renderImage(ImageData data) {
        log("將圖片繪製到畫面上： " + data.getData());
    }

    // 資料結構類別
    static class ImageInfo {
        private final String url;

        public ImageInfo(String url) {
            this.url = url;
        }

        public ImageData downloadImage() {
            log("開始下載圖片： " + url);
            try {
                // 模擬每張圖片下載需要 1.5 秒
                Thread.sleep(1500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            log("圖片下載完成： " + url);
            return new ImageData("ImageDataFor[" + url + "]");
        }
    }

    static class ImageData {
        private final String data;

        public ImageData(String data) {
            this.data = data;
        }

        public String getData() {
            return data;
        }
    }


}
