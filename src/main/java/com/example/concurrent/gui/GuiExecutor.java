package com.example.concurrent.gui;

import javax.swing.*;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.TimeUnit;

public class GuiExecutor extends AbstractExecutorService {
    // 單例模式
    private static final GuiExecutor instance = new GuiExecutor();

    private GuiExecutor() {}

    public static GuiExecutor instance() {
        return instance;
    }

    @Override
    public void shutdown() {

    }

    @Override
    public List<Runnable> shutdownNow() {
        return List.of();
    }

    @Override
    public boolean isShutdown() {
        return false;
    }

    @Override
    public boolean isTerminated() {
        return false;
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return false;
    }

    // 我不確定現在這行程式碼是在背景執行緒還是在 EDT，但我希望它一定在 EDT 安全執行
    @Override
    public void execute(Runnable command) {
        if (SwingUtilities.isEventDispatchThread()) command.run(); // 如果當前已經在 GUI 執行緒 (EDT)，直接執行
        else SwingUtilities.invokeLater(command);
    }
}
