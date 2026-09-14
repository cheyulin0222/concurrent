package com.example.concurrent.test;

import java.util.concurrent.Semaphore;

// 投幣式置物櫃系統
// N 個 置物櫃 (capacity)
public class BounderBuffer<E> {
    // Semaphore 通行證發放機，放者固定數量的代幣
    // acquire 索取一枚代幣，若當前沒代幣，則執行緒進入阻塞
    // release 歸還代幣，有在排隊的人就會被喚醒拿到代幣
    // 可用空位，可用物品
    private final Semaphore availableItems, availableSpaces;
    // 真正的容器陣列
    private final E[] items;
    // 下一個要存入物品的位置索引
    // 下一個要取出物品的位置索引
    private int putPosition = 0, takePosition = 0;

    public BounderBuffer(int capacity) {
        availableItems = new Semaphore(0);
        availableSpaces = new Semaphore(capacity);
        items = (E[]) new Object[capacity];
    }

    public boolean isEmpty() {
        return availableItems.availablePermits() == 0;
    }

    public boolean isFull() {
        return availableSpaces.availablePermits() == 0;
    }

    public void put(E x) throws InterruptedException {
        availableSpaces.acquire();
        doInsert(x);
        availableItems.release();
    }

    public E take() throws InterruptedException {
        availableItems.acquire();
        E item = deExtract();
        availableSpaces.release();
        return item;
    }

    private synchronized void doInsert(E x) {
        int i = putPosition;
        items[i] = x;
        // 每放一個東西，指標就往後移一格。如果已經走到陣列的最末端，下一次就繞回開頭的第 0 格重複利用。
        putPosition = (++i == items.length) ? 0 : i;
    }

    private synchronized E deExtract() {
        int i = takePosition;
        E x = items[i];
        items[i] = null;
        takePosition = (++i == items.length) ? 0 : i;
        return x;
    }

}
