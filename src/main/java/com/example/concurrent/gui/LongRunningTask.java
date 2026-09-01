package com.example.concurrent.gui;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LongRunningTask {
    
    private static final ExecutorService backgroundExec = Executors.newCachedThreadPool();

    // 模擬耗時的大量運算（如網路請求、大檔案讀寫、複雜計算）
    private static void doBigComputation() {
        try {
            System.out.println("  [背景] 開始執行耗時運算... 執行緒: " + Thread.currentThread().getName());
            Thread.sleep(3000);
            System.out.println("  [背景] 耗時運算完成！");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    static void main() {
        JFrame frame = new JFrame("Listing 9.4 & 9.5 長時間任務範例");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new GridLayout(2, 1, 10, 10));

        // ---------------------------------------------------------------------
        // 區塊 1: Listing 9.4 範例 (Fire-and-Forget 丟了不管)
        // ---------------------------------------------------------------------
        JPanel panel94 = new JPanel(new FlowLayout());
        JButton btn94 = new JButton("9.4 觸發背景任務 (無 UI 回饋)");
        panel94.setBorder(BorderFactory.createTitledBorder("Listing 9.4: Fire and Forget"));
        panel94.add(btn94);

        btn94.addActionListener(e -> {
            System.out.println("\n[9.4] 按鈕被點擊，準備把任務丟給背景執行緒...");
            backgroundExec.execute(LongRunningTask::doBigComputation);
            System.out.println("[9.4] 任務已提交！EDT 立即釋放，UI 保持可回應狀態。");
        });

        // ---------------------------------------------------------------------
        // 區塊 2: Listing 9.5 範例 (帶有 UI 進度/狀態回饋)
        // ---------------------------------------------------------------------
        JPanel panel95 = new JPanel(new FlowLayout());
        JButton btn95 = new JButton("9.5 開始計算");
        JLabel label95 = new JLabel("Status: idle");
        panel95.setBorder(BorderFactory.createTitledBorder("Listing 9.5: 帶有 UI 回饋"));
        panel95.add(btn95);
        panel95.add(label95);

        btn95.addActionListener(e -> {
            System.out.println("\n[9.5] 按鈕被點擊，開始三階段執行緒跳躍...");

            // 階段 1 (在 EDT 中)：修改 UI，提示使用者「處理中」並停用按鈕
            btn95.setEnabled(false);
            label95.setText("Status: busy...");

            backgroundExec.execute(() -> {
                try {
                    doBigComputation();
                } finally {
                    GuiExecutor.instance().execute(() -> {
                        System.out.println("[9.5] 轉回 EDT，更新 UI 為完成狀態... 執行緒: " + Thread.currentThread().getName());
                        btn95.setEnabled(true);
                        label95.setText("Status: idle (完成!)");
                    });
                }
            });
        });

        frame.add(panel94);
        frame.add(panel95);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

}
