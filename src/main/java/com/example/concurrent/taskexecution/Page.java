package com.example.concurrent.taskexecution;

public class Page {

    private String body;
    private Ad ad;

    public Page(String body) {
        this.body = body;
    }

    public void setAd(Ad ad) {
        this.ad = ad;
    }

    public void display() {
        System.out.println("\n========== 網頁渲染結果 ==========");
        System.out.println("頁面內容: " + body);
        System.out.println("廣告區塊: " + ad);
        System.out.println("===================================\n");
    }
}
