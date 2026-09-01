package com.example.concurrent.gui;

import javax.swing.*;
import java.awt.*;
import java.util.Random;

public class ShortRunningTask {

    static void main() {
        // Swing 規範：建立與顯示 UI 必須排入 EDT (Event Dispatch Thread) 中執行
        SwingUtilitiesDemo.invokeLater(ShortRunningTask::createAndShowGUI);
    }

    private static void createAndShowGUI() {
        JFrame frame = new JFrame("Listing 9.3 - 短任務範例");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(300, 200);
        frame.setLayout(new FlowLayout());

        // 2. 建立按鈕與隨機數產生器 (Listing 9.3 的核心)
        final Random random = new Random();
        final JButton button = new JButton("Change Color");

        button.addActionListener(e -> {
            // 檢查並印出當前執行這行程式碼的執行緒名稱
            System.out.println("按鈕被點擊！當前執行緒: " + Thread.currentThread().getName());

            // 隨機產生顏色並修改按鈕背景色
            button.setBackground(new Color(random.nextInt()));
        });

        // 4. 將按鈕放入視窗並顯示
        frame.add(button);
        frame.setLocationRelativeTo(null); // 視窗置中
        frame.setVisible(true);
    }
}
