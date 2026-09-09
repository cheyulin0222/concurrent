package com.example.concurrent.liveinghazards;

public class LeftRightDeadlock {

    private final Object left = new Object();
    private final Object right = new Object();

    public void leftRight() {
        synchronized (left) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException ignore) {}
            synchronized (right) {
                doSomething();
            }
        }
    }

    public void rightLeft() {
        synchronized (right) {
            try { Thread.sleep(10); } catch (InterruptedException ignored) {}
            synchronized (left) {
                doSomethingElse();
            }
        }
    }

    private void doSomethingElse() {
        System.out.println("rightToLeft");
    }

    private void doSomething() {
        System.out.println("leftToRight");
    }

    public static void main(String[] args) {

        LeftRightDeadlock deadlockDemo = new LeftRightDeadlock();

        Thread threadA = new Thread(() -> {
            while (true) {
                deadlockDemo.leftRight();
            }
        }, "Thread-LeftRight");

        Thread threadB = new Thread(() -> {
            while (true) {
                deadlockDemo.rightLeft();
            }
        }, "Thread-RightLeft");

        threadA.start();
        threadB.start();
    }
}
