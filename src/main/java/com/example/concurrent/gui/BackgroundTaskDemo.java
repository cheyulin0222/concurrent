package com.example.concurrent.gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BackgroundTaskDemo {
    // 背景執行
    private static final ExecutorService backgroundExec = Executors.newCachedThreadPool();

    // =========================================================================
    // Listing 9.8: GUI 視窗主程式
    // =========================================================================

    static void main() {
        SwingUtilities.invokeLater(BackgroundTaskDemo::createAndShowGUI);
    }

    private static void createAndShowGUI() {
        JFrame frame = new JFrame("Listing 9.7 & 9.8 - BackgroundTask 範例");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new FlowLayout(FlowLayout.CENTER, 10, 10));

        JButton startButton = new JButton("Start Task");
        JButton cancelButton = new JButton("Cancel Task");
        cancelButton.setEnabled(false); // 初始時沒有任務，停用取消按鈕

        JLabel label = new JLabel("Status: Idle");
        JProgressBar progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);

        // 點擊 Start Button (Listing 9.8 模式)
        startButton.addActionListener(e -> {
            startButton.setEnabled(false);
            cancelButton.setEnabled(true);
            label.setText("Status: Running...");
            progressBar.setValue(0);

            // 定義一個專屬的 CancelListener，將按鈕與任務綁定
            class CancelListener implements ActionListener {
                BackgroundTask<?> task;

                @Override
                public void actionPerformed(ActionEvent e) {
                    if (task != null) task.cancel(true);
                }
            }

            final CancelListener listener = new CancelListener();

            // 建立具體的 BackgroundTask 任務
            listener.task = new BackgroundTask<String>() {

                // 1. 在【背景執行緒】中運算
                @Override
                protected String compute() throws Exception {
                    int progress = 0;
                    // 只要專注於業務邏輯
                    while (progress < 100 && !isCancelled()) {
                        Thread.sleep(300);
                        progress += 10;
                        // 回報進度
                        setProgress(progress, 100);
                    }
                    return "運算成果資料 #888";
                }

                // 2. 在【EDT 執行緒】中即時更新進度
                @Override
                protected void onProgress(int current, int max) {
                    progressBar.setValue(current);
                }

                // 3. 在【EDT 執行緒】中接收最終結果或取消狀態
                @Override
                public void onCompletion(String result, Throwable exception, boolean cancelled) {
                    // 任務結束：自動解除 CancelListener
                    cancelButton.removeActionListener(listener);
                    cancelButton.setEnabled(false);
                    startButton.setEnabled(true);

                    if (cancelled) {
                        label.setText("Status: Cancelled!");
                    } else if (exception != null) {
                        label.setText("Status: Error (" + exception.getMessage() + ")");
                    } else {
                        label.setText("Status: Done! Result: " + result);
                    }
                }
            };

            // 將取消監聽器掛到 Cancel 按鈕上，並將 Task 提交給背景執行緒池
            cancelButton.addActionListener(listener);
            backgroundExec.execute(listener.task);
        });

        frame.add(startButton);
        frame.add(cancelButton);
        frame.add(label);
        frame.add(progressBar);

        frame.pack();
        frame.setSize(420, 150);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}
