package com.example.concurrent.gui;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;
import java.util.concurrent.*;

public class BrokenTestThreadSafeEnhance {

    private static final ExecutorService backgroundExec = Executors.newCachedThreadPool();
    // 1. 真正的 Thread-Safe 核心資料模型：供多個背景 Worker 同時安全寫入
    // Key: 節點唯一識別碼 (避免覆蓋), Value: 節點顯示文字
    private static final ConcurrentMap<String, String> sharedDataModel = new ConcurrentHashMap<>();


    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> render());
    }

    private static void render() {
        JFrame frame = new JFrame("❌ 錯誤示範：背景執行緒直接改 TreeModel");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        DefaultMutableTreeNode root = new DefaultMutableTreeNode("Remote-Server");
        DefaultMutableTreeNode targetFolder = new DefaultMutableTreeNode("High-Traffic-Folder");
        root.add(targetFolder);

        DefaultTreeModel treeModel = new DefaultTreeModel(root);
        JTree tree = new JTree(treeModel);

        JButton testBtn = new JButton("啟動 10 個執行緒同時塞入共 1000 個節點");
        JLabel resultLabel = new JLabel("預期數量: 1000 筆 | 實際數量: 等待測試...");
        resultLabel.setFont(new Font("Monospaced", Font.BOLD, 14));

        testBtn.addActionListener(e -> {
            testBtn.setEnabled(false);
            targetFolder.removeAllChildren();
            treeModel.nodeStructureChanged(targetFolder);
            sharedDataModel.clear(); // 清空舊資料
            resultLabel.setText("正在併發寫入中...");

            int threadCount = 10;
            int tasksPerThread = 100;
            int expectedTotal = threadCount * tasksPerThread; // 預期 1000 筆

            CountDownLatch countDownLatch = new CountDownLatch(threadCount);

            for (int i = 0; i < threadCount; i++) {
                final int threadId = i;
                backgroundExec.execute(() -> {
                    try {
                        for (int j = 0; j < tasksPerThread; j++) {
                            String key = "T" + threadId + "-" + j;
                            String value = "File-Data-" + threadId + "-" + j;

                            sharedDataModel.put(key, value);
                        }
                    } finally {
                        countDownLatch.countDown();
                    }
                });
            }

            // 另外開一個執行緒等待所有 Worker 做完，並清點實際數量
            backgroundExec.execute(() -> {
                try {
                    countDownLatch.await(); // 等待 10 個背景執行緒全數跑完

                    // 轉回 EDT 檢查最後的真實節點數
                    SwingUtilities.invokeLater(() -> {
                        sharedDataModel.forEach((key, value) -> {
                            DefaultMutableTreeNode child = new DefaultMutableTreeNode(key + "(" + value + ")");
                            targetFolder.add(child);
                        });
                    });

                    // 一次性通知 JTree 重繪
                    treeModel.nodeStructureChanged(targetFolder);

                    int actualCount = targetFolder.getChildCount();
                    testBtn.setEnabled(true);

                    if (actualCount == expectedTotal) {
                        resultLabel.setText("<html><font color='blue'>✅ 成功！實際數量: " + actualCount + " / " + expectedTotal + " (完全無遺失)</font></html>");
                    } else {
                        resultLabel.setText("<html><font color='red'>💥 異常: " + actualCount + "</font></html>");
                    }

                } catch (InterruptedException ignored) {}
            });
        });


        frame.add(new JScrollPane(tree), BorderLayout.CENTER);
        JPanel southPanel = new JPanel(new GridLayout(2, 1));
        southPanel.add(testBtn);
        southPanel.add(resultLabel);
        frame.add(southPanel, BorderLayout.SOUTH);

        frame.setSize(520, 400);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}
