package com.example.concurrent.gui;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BrokenTreeConcurrencyDemo {
    
    private static final ExecutorService backgroundPool = Executors.newCachedThreadPool();
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(BrokenTreeConcurrencyDemo::createAndShowGUI);
    }

    private static void createAndShowGUI() {
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
            resultLabel.setText("正在併發寫入中...");

            int threadCount = 10;
            int itemsPerThread = 100;
            int expectedTotal = threadCount * itemsPerThread; // 預期 1000 筆

            CountDownLatch latch = new CountDownLatch(threadCount);

            // 同時啟動 10 個背景任務模擬多個非同步網路請求回傳
            for (int t = 0; t < threadCount; t++) {
                final int threadId = t;
                backgroundPool.execute(() -> {
                    try {
                        for (int i = 0; i < itemsPerThread; i++) {
                            DefaultMutableTreeNode child = new DefaultMutableTreeNode("T" + threadId + "-" + i);

                            // ❌ 違規：多執行緒同時 getChildCount() 與 insertNodeInto
                            int currentCount = targetFolder.getChildCount();
                            treeModel.insertNodeInto(child, targetFolder, currentCount);
                        }
                    } catch (Exception ex) {
                        System.err.println("拋出例外: " + ex);
                    } finally {
                        latch.countDown();
                    }
                });
            }

            // 另外開一個執行緒等待所有 Worker 做完，並清點實際數量
            backgroundPool.execute(() -> {
                try {
                    latch.await(); // 等待 10 個背景執行緒全數跑完

                    // 轉回 EDT 檢查最後的真實節點數
                    SwingUtilities.invokeLater(() -> {
                        int actualCount = targetFolder.getChildCount();
                        testBtn.setEnabled(true);

                        if (actualCount == expectedTotal) {
                            resultLabel.setText("<html><font color='blue'>數量正確: " + actualCount + " / " + expectedTotal + " (運氣好沒撞車)</font></html>");
                        } else {
                            resultLabel.setText("<html><font color='red'>💥 發生資料遺失！預期: " + expectedTotal + " 筆，實際只有: " + actualCount + " 筆！</font></html>");
                        }
                    });
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
