package com.example.concurrent.nonblocking;

import java.util.concurrent.atomic.AtomicReference;

public class LinkedQueue<E> {

    private final Node<E> dummy = new Node<>(null, null);
    private final AtomicReference<Node<E>> head = new AtomicReference<>(dummy);
    private final AtomicReference<Node<E>> tail = new AtomicReference<>(dummy);

    public boolean put(E item) {
        Node<E> newNode = new Node<>(item, null);
        while (true) {
            Node<E> curTail = tail.get();
            Node<E> tailNext = curTail.next.get();

            // 雙重檢查：確認在此期間 tail 沒有被其他執行緒換掉
            if (curTail == tail.get()) {

                // 【情境 1：中間狀態（Intermediate State）】
                // tail.next 不是 null，代表有其他執行緒已經把節點掛上去了，但還沒更新 tail 指針
                if (tailNext != null) {
                    // 【步驟 B：協助機制】
                    // 當前執行緒不乾等，主動「幫」對方把 tail 往前推移到 tailNext
                    tail.compareAndSet(curTail, tailNext);
                }
            }

            // 【情境 2：靜止狀態（Quiescent State）】
            // tail.next 為 null，隊尾處於正常狀態，可以嘗試插入新節點
            else {
                // 【步驟 C：關鍵入隊 CAS】
                // 嘗試將當前尾節點的 next 從 null 指向 newNode
                if (curTail.next.compareAndSet(null, newNode)) {
                    // 【步驟 D：善後工作】
                    // 入隊已實質成功！嘗試將 tail 推進指向 newNode
                    // 即使這裡的 CAS 失敗也無所謂（return true），因為其他執行緒會在步驟 B 幫忙推進！
                    tail.compareAndSet(curTail, newNode);
                    return true;
                }
            }
        }
    }

    private static class Node<E> {
        final E item;
        final AtomicReference<Node<E>> next;

        public Node(E item, Node<E> next) {
            this.item = item;
            this.next = new AtomicReference<>(next);
        }
    }
}
