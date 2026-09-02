package com.example.concurrent.gui;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BrokenTest {

    private static final ExecutorService backgroundExec = Executors.newCachedThreadPool();


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
            targetFolder.removeAllChildren();
            treeModel.nodeStructureChanged(targetFolder);
            resultLabel.setText("正在併發寫入中...");

            int threadCount = 10;
            int tasksPerThread = 100;
            int expectedTotal = threadCount * tasksPerThread; // 預期 1000 筆

            CountDownLatch countDownLatch = new CountDownLatch(expectedTotal);

            for (int i = 0; i < threadCount; i++) {
                final int threadId = i;
                try {
                    backgroundExec.execute(() -> {
                        for (int j = 0; j < tasksPerThread; j++) {
                            DefaultMutableTreeNode child = new DefaultMutableTreeNode("T" + threadId + "-" + j);

                            // ❌ 違規：多執行緒同時 getChildCount() 與 insertNodeInto
                            int currentCount = targetFolder.getChildCount();
                            treeModel.insertNodeInto(child, targetFolder, currentCount);
                        }
                    });
                } catch (Exception ex) {
                    ex.printStackTrace();
                } finally {
                    countDownLatch.countDown();
                }
            }

            // 另外開一個執行緒等待所有 Worker 做完，並清點實際數量
            backgroundExec.execute(() -> {
                try {
                    countDownLatch.await(); // 等待 10 個背景執行緒全數跑完

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
