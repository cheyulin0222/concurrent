package com.example.concurrent.taskexecution;

import java.util.concurrent.Executor;

public class SingleThreadExecutor implements Executor {
    @Override
    public void execute(Runnable command) {
        command.run();
    }
}
