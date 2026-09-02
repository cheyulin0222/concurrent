package com.example.concurrent.gui;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.*;

// 沒有 sharedBuffer 的話
// 1. 事件佇列被「洪水（Flooding）」淹沒：
// 2. EDT 發生重繪風暴（Repaint Storm）：
// EDT 必須切換執行緒上下文、依序從佇列拿出這 10,000 個小任務。更糟的是，每次插入都會觸發 nodeStructureChanged，逼著 EDT 一萬次去重新計算樹節點的幾何尺寸並呼叫繪圖函式。
// 3. UI 反應極度遲鈍（Laggy）：
// 因為信箱裡堆了一萬封信，此時使用者如果嘗試點擊其他按鈕、移動滑鼠、拖曳視窗，使用者的事件會被排在這一萬封信的最後面，導致視窗看起來完全卡死、沒有回應。
// 4. 大量垃圾回收壓力（GC Overhead）：
// 瞬間產生 10,000 個微型 Runnable 和 Event 物件，造成 JVM 記憶體頻繁觸發 GC。

public class BrokenTestSplitModelEnhance {

    private static final ExecutorService backgroundExec = Executors.newCachedThreadPool();

    // =========================================================================
    // 1. Shared / Application Model (領域模型)：執行緒安全，供背景執行緒高速寫入
    // =========================================================================
    static class SharedApplicationModel {
        // 使用高效的無鎖佇列收集資料
        private final Queue<String> sharedBuffer = new ConcurrentLinkedQueue<>();

        public void addData(String item) {
            sharedBuffer.offer(item);
        }

        // 取出當前佇列中累積的所有資料（排空批次）
        public List<String> drainBatch() {
            List<String> batch = new ArrayList<>();
            String item;

            while ((item = sharedBuffer.poll()) != null) {
                batch.add(item);
            }
            return batch;
        }

        public void clear() {
            sharedBuffer.clear();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(BrokenTestSplitModelEnhance::render);
    }

    private static void render() {
        JFrame frame = new JFrame("❌ 錯誤示範：背景執行緒直接改 TreeModel");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        // =====================================================================
        // 2. Presentation Model (呈現模型)：嚴格限制在 EDT，專門負責視窗渲染
        // =====================================================================
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("Remote-Server");
        DefaultMutableTreeNode targetFolder = new DefaultMutableTreeNode("High-Traffic-Folder");
        root.add(targetFolder);

        DefaultTreeModel treeModel = new DefaultTreeModel(root);
        JTree tree = new JTree(treeModel);

        JButton testBtn = new JButton("啟動 10 個執行緒同時塞入共 1000 個節點");
        JLabel resultLabel = new JLabel("預期數量: 1000 筆 | 實際數量: 等待測試...");
        resultLabel.setFont(new Font("Monospaced", Font.BOLD, 14));

        SharedApplicationModel appModel = new SharedApplicationModel();

        testBtn.addActionListener(e -> {
            testBtn.setEnabled(false);
            targetFolder.removeAllChildren();
            treeModel.nodeStructureChanged(targetFolder);
            appModel.clear(); // 清空舊資料
            resultLabel.setText("正在併發寫入中...");

            int threadCount = 10;
            int tasksPerThread = 100;
            int expectedTotal = threadCount * tasksPerThread; // 預期 1000 筆
            int batchSize = 50;  // 每累積 50 筆做一次增量更新

            CountDownLatch countDownLatch = new CountDownLatch(threadCount);

            for (int i = 0; i < threadCount; i++) {
                final int threadId = i;
                backgroundExec.execute(() -> {
                    try {
                        for (int j = 0; j < tasksPerThread; j++) {
                            // 1. 寫入背景的 Application Model
                            appModel.addData("T" + threadId + "-" + j);

                            if ((j + 1) % batchSize == 0) {
                                List<String> batch = appModel.drainBatch();
                                if (!batch.isEmpty()) {
                                    dispatchIncrementalUpdate(batch, targetFolder, treeModel, resultLabel);
                                }
                            }

                            try {
                                // 模擬真實環境微小的 IO 延遲
                                Thread.sleep(2);
                            } catch (InterruptedException ex) {
                                Thread.currentThread().interrupt();
                            }
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

                    // 排空最後剩下的資料
                    List<String> remaining = appModel.drainBatch();

                    if (!remaining.isEmpty()) {
                        dispatchIncrementalUpdate(remaining, targetFolder, treeModel, resultLabel);
                    }

                    // 轉回 EDT 檢查最後的真實節點數
                    SwingUtilities.invokeLater(() -> {
                        testBtn.setEnabled(true);
                        int actualCount = targetFolder.getChildCount();
                        if (actualCount == expectedTotal) {
                            resultLabel.setText("<html><font color='blue'>✅ 成功！實際數量: " + actualCount + " / " + expectedTotal + " (完全無遺失)</font></html>");
                        } else {
                            resultLabel.setText("<html><font color='red'>💥 異常: " + actualCount + "</font></html>");
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

    private static void dispatchIncrementalUpdate(List<String> batchData,
                                                  DefaultMutableTreeNode parentNode,
                                                  DefaultTreeModel treeModel,
                                                  JLabel statusLabel) {

        SwingUtilities.invokeLater(() -> {
            // 此處執行在 EDT：只有 EDT 會碰 JTree 的 Presentation Model
            for (String item : batchData) {
                parentNode.add(new DefaultMutableTreeNode(item));
            }

            // 局部刷新，畫面會平滑逐批長出節點
            treeModel.nodeStructureChanged(parentNode);
            statusLabel.setText("狀態: 增量更新中... 目前節點數: " + parentNode.getChildCount());
        });
    }
}
