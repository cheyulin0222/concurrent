package com.example.concurrent.gui;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ThreadSafeModeDemo {

    // 1. 執行緒安全的共用資料儲存庫 (Shared Thread-Safe Repository)
    // 背景執行緒可隨時寫入，EDT 也可以隨時讀取
    private static final ConcurrentMap<String, List<String>> remoteFileRepository = new ConcurrentHashMap<>();
    
    private static final ExecutorService backgroundPool = Executors.newCachedThreadPool();
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(ThreadSafeModeDemo::createAndShowGUI);
    }

    private static void createAndShowGUI() {
        JFrame frame = new JFrame("方案一：Thread-Safe Data Model (Lazy Tree)");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        DefaultMutableTreeNode root = new DefaultMutableTreeNode("Remote-Root");
        DefaultMutableTreeNode nodeA = new DefaultMutableTreeNode("Folder-A (點我載入)");
        DefaultMutableTreeNode nodeB = new DefaultMutableTreeNode("Folder-B (點我載入)");
        root.add(nodeA);
        root.add(nodeB);

        DefaultTreeModel treeModel = new DefaultTreeModel(root);
        JTree tree = new JTree(treeModel);

        JButton loadBtn = new JButton("非同步載入 Folder-A 的遠端子項目");
        JLabel statusLabel = new JLabel("狀態: 閒置 (UI 保持完全流暢)");

        loadBtn.addActionListener(e -> {
            loadBtn.setEnabled(false);
            statusLabel.setText("狀態: 背景抓取遠端資料中 (1.5秒)...");

            backgroundPool.execute(() -> {
                try {
                    // 模擬耗時的遠端網路呼叫
                    Thread.sleep(1500);

                    // 安全地寫入 Thread-Safe 的 ConcurrentHashMap
                    remoteFileRepository.put("Folder-A", List.of("File-A1.txt", "File-A2.png", "File-A3.pdf"));

                    // 3. 抓取完畢，只需通知 EDT 根據共用模型更新 UI 樹狀節點
                    SwingUtilities.invokeLater(() -> {
                        List<String> files = remoteFileRepository.get("Folder-A");
                        nodeA.removeAllChildren();
                        for (String file : files) {
                            nodeA.add(new DefaultMutableTreeNode(file));
                        }
                        nodeA.setUserObject("Folder-A (已載入)");
                        treeModel.nodeStructureChanged(nodeA); // 通知 JTree 重新繪製局部
                        tree.expandRow(1);

                        loadBtn.setEnabled(true);
                        statusLabel.setText("狀態: Folder-A 載入完成！");
                    });
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            });

            frame.add(new JScrollPane(tree), BorderLayout.CENTER);
            JPanel bottomPanel = new JPanel(new GridLayout(2, 1));
            bottomPanel.add(loadBtn);
            bottomPanel.add(statusLabel);
            frame.add(bottomPanel, BorderLayout.SOUTH);

            frame.setSize(400, 300);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

        });
    }


}
