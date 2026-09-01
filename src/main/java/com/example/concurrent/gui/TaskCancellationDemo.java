package com.example.concurrent.gui;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class TaskCancellationDemo {

    // 背景執行緒池，用來執行耗時任務
    private static final ExecutorService backgroundExec = Executors.newCachedThreadPool();
    // 限制在 EDT 中存取的任務引用（無需額外加鎖同步）
    private static Future<?> runningTask = null;

    static void main() {
        // 使用 Swing 官方正牌的 SwingUtilities 在 EDT 中建立 UI
        SwingUtilities.invokeLater(TaskCancellationDemo::createAndShowGUI);
    }

    private static void createAndShowGUI() {
        JFrame frame = new JFrame("Listing 9.6 - 任務取消範例");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new FlowLayout());

        JButton startButton = new JButton("Start Task");
        JButton cancelButton = new JButton("Cancel Task");
        JLabel statusLabel = new JLabel("Status: Ready");

        startButton.addActionListener(e -> {
            // 在 EDT 中檢查：確保同一時間只有一個背景任務在執行
            if (runningTask == null) {
                statusLabel.setText("Status: Running...");
                System.out.println("[EDT] 開始提交背景任務...");

                // 提交任務並將返回的 Future 賦值給 runningTask
                runningTask = backgroundExec.submit(() -> {
                    try {
                        int progress = 0;

                        // 缺點 : 必須自己小心處理 Thread Interruption 與 InterruptedException
                        // 模擬需要做 100 次的迴圈工作 (moreWork)
                        while (progress < 100) {
                            // 核心：檢查自己是否被中斷/取消 (isInterrupted)
                            if (Thread.currentThread().isInterrupted()) {
                                System.out.println("  [背景] 偵測到中斷訊號！開始清理資源 (cleanUpPartialWork)...");
                                cleanUpPartialWork();
                                break;
                            }

                            // 模擬實際工作 (doSomeWork)
                            doSomeWork();
                            progress += 10;
                            System.out.println("  [背景] 任務處理進度: " + progress + "%");
                        }
                    } finally {
                        // 缺點 : 到處都是樣板程式碼）：
                        //每次背景算到一半要更新進度、或是算完要改 UI，都要手動寫一次：
                        // 任務結束（無論是正常做完還是被取消），轉回 EDT 重置狀態
                        SwingUtilities.invokeLater(() -> {
                            System.out.println("[EDT] 背景任務正式結束，清除 runningTask");
                            runningTask = null;
                            statusLabel.setText("Status: Stopped/Completed");
                        });
                    }
                });
            } else {
                System.out.println("[EDT] 任務已經在執行中，忽略點擊。");
            }
        });

        // ---------------------------------------------------------------------
        // 2. 取消按鈕監聽器
        // ---------------------------------------------------------------------
        cancelButton.addActionListener(e -> {
            // 在 EDT 中檢查：如果目前有任務在跑，就發送取消指令
            if (runningTask != null) {
                System.out.println("[EDT] 發送 cancel(true) 指令中斷任務！");
                // mayInterruptIfRunning = true：向正在執行的背景執行緒發送 interrupt 訊號
                runningTask.cancel(true);
            } else {
                System.out.println("[EDT] 當前沒有正在執行的任務。");
            }
        });

        frame.add(startButton);
        frame.add(cancelButton);
        frame.add(statusLabel);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private static void cleanUpPartialWork() {
        System.out.println("  [背景] 清理工作完成。");
    }

    // 模擬工作中的子階段
    private static void doSomeWork() {
        try {
            // 模擬每小段工作耗時 0.5 秒
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

}

