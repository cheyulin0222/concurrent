package com.example.concurrent.taskexecution;

import java.time.LocalTime;

public class LOG {

    public static void log(String msg) {
        System.out.println("[" + LocalTime.now() + "] " + msg);
    }
}
