package com.example.concurrent.taskexecution;

import java.util.concurrent.Callable;

// 詢價任務
public class QuoteTask implements Callable<TravelQuote> {
    private final TravelCompany company;

    public QuoteTask(TravelCompany company) {
        this.company = company;
    }

    @Override
    public TravelQuote call() throws Exception {
        return company.solicitQuote();
    }

    public  TravelQuote getTimeoutQuote() {
        return new TravelQuote(company.getName(), "響應逾時 (超過預算)");
    }

    public TravelQuote getFailureQuote(Throwable cause) {
        return new TravelQuote(company.getName(), "❌ 系統異常: " + cause.getMessage());
    }
}
