package com.example.concurrent.taskexecution;

import java.util.concurrent.Executor;

public class ThreadPerTaskExecutor implements Executor {
    @Override
    public void execute(Runnable command) {
        new Thread(command).start();
    }
}
