package com.example.concurrent.component;

public class Ad {
    private final String content;

    public Ad(String content) {
        this.content = content;
    }

    @Override
    public String toString() { return content; }
}
