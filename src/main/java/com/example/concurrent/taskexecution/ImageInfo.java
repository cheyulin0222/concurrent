package com.example.concurrent.taskexecution;

public class ImageInfo {

    private final String url;

    public ImageInfo(String url) {
        this.url = url;
    }

    public ImageData downloadImage() {
        LOG.log("開始下載圖片： " + url);
        try {
            // 模擬每張圖片下載需要 1.5 秒
            Thread.sleep(1500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        LOG.log("圖片下載完成： " + url);
        return new ImageData("ImageDataFor[" + url + "]");
    }
}
