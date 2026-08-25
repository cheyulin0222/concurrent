package com.example.concurrent.taskexecution;

// 報價結果
public class TravelQuote {
    private final String companyName;
    private final double price;
    private final String errorReason;

    public TravelQuote(String companyName, double price) {
        this.companyName = companyName;
        this.price = price;
        this.errorReason = null;
    }

    public TravelQuote(String companyName, String errorReason) {
        this.companyName = companyName;
        this.price = Double.MAX_VALUE; // 設定無效價格
        this.errorReason = errorReason;
    }

    public double getPrice() {
        return price;
    }

    @Override
    public String toString() {
        if (errorReason != null) {
            return String.format("[%s] 拿不到報價 (%s)", companyName, errorReason);
        }
        return String.format("[%s] 報價: $%.0f", companyName, price);
    }


}
