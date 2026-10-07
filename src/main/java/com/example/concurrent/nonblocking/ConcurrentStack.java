package com.example.concurrent.nonblocking;

import org.w3c.dom.Node;

import java.util.concurrent.atomic.AtomicReference;

public class ConcurrentStack<E> {

    AtomicReference<Node<E>> top = new AtomicReference<Node<E>>();

    public void push(E item) {
        Node<E> newHead = new Node<>(item);
        Node<E> oldHead;

        do {
            oldHead = top.get();        // 1. 取得當前棧頂快照
            newHead.next = oldHead;     // 2. 投機性地將新節點的 next 指向快照
        // 3. 嘗試以 CAS 將棧頂替換為 newHead：
        //    - 若 top 仍為 oldHead：更新成功，跳出迴圈
        //    - 若 top 已被其他執行緒改動：CAS 失敗，重新讀取最新 top 重試
        } while (!top.compareAndSet(oldHead, newHead));
    }

    public E pop() {
        Node<E> oldHead;
        Node<E> newHead;

        do {
            oldHead = top.get();       // 1. 取得當前棧頂快照
            if (oldHead == null)        // 若堆棧為空，直接返回 null
                return null;
            newHead = oldHead.next;     // 2. 預定潭處後的下一個棧頂

        // 3. 嘗試以 CAS 將 top 更新為 newHead：
        //    - 若棧頂未被動過：成功彈出 oldHead，跳出迴圈
        //    - 若棧頂已被動過：失敗重試
        } while (!top.compareAndSet(oldHead, newHead));

        return oldHead.item;    // 返回被彈出節點的值
    }

    private static class Node<E> {
        public final E item;
        public Node<E> next;

        public Node(E item) {
            this.item = item;
        }
    }
}
