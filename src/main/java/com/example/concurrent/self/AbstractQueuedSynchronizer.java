package com.example.concurrent.self;

import sun.misc.Unsafe;

import java.util.concurrent.locks.AbstractOwnableSynchronizer;
import java.util.concurrent.locks.LockSupport;

public class AbstractQueuedSynchronizer extends AbstractOwnableSynchronizer {
    static final int WAITING = 1;

    // 2. FIFO 等待佇列的頭節點（指向當前拿到鎖或哨兵節點）
    private transient volatile Node head;
    // 3. FIFO 等待佇列的尾節點（新來搶鎖失敗的節點會排到這裡）
    private transient volatile Node tail;
    // 1. 同步狀態（最重要的靈魂整數）
    private volatile int state;

    // Unsafe
    private static final Unsafe U = Unsafe.getUnsafe();
    private static final long STATE
            = U.objectFieldOffset(AbstractQueuedSynchronizer.class, "state");
    private static final long HEAD
            = U.objectFieldOffset(java.util.concurrent.locks.AbstractQueuedSynchronizer.class, "head");
    private static final long TAIL
            = U.objectFieldOffset(java.util.concurrent.locks.AbstractQueuedSynchronizer.class, "tail");

    protected final int getState() { return state; }
    protected final void setState(int newState) { state = newState; }
    protected final boolean compareAndSetState(int expect, int update) {
        return U.compareAndSetInt(this, STATE, expect, update);
    }

    // 骨架方法 (final，給業務呼叫，全包了排隊與阻塞細節)
    public final void acquire(int arg) {
        // 入隊前的最後垂死掙扎
        // 這一瞬間，前一個持鎖者剛好把鎖放開了，這條執行緒就能立刻拿到鎖直接返回，連佇列都不用進
        // 依然失敗，取反後為 true，繼續往下執行 addWaiter(...)
        if (!tryAcquire(arg)) {
            acquire(null, arg, false, false, false, 0L);
        }
    }

    public final boolean release(int arg) {
        // 回傳 0
        if (tryRelease(arg)) {
            signalNext(head);
            return true;
        }
        // 可重入，所以不一定為 0
        return false;
    }

    public final void acquireShared(int arg) {
        if (tryAcquireShared(arg) < 0)
            acquire(null, arg, true, false, false, 0L);
    }

    public final boolean releasShared(int arg) {
        if (tryReleaseShared(arg)) {
            signalNext(head);
            return true;
        }
        return false;
    }

    // 子類別需要覆寫「鉤子方法」(定義通行規則)
    protected boolean tryAcquire(int arg) {
        throw new UnsupportedOperationException();
    }
    protected boolean tryRelease(int arg) {
        throw new UnsupportedOperationException();
    }
    protected int tryAcquireShared(int arg) {
        throw new UnsupportedOperationException();
    }
    protected boolean tryReleaseShared(int arg) {
        throw new UnsupportedOperationException();
    }


    final int acquire(Node node, int arg, boolean shared,
                      boolean interruptible, boolean timed, long time) {
        Thread current = Thread.currentThread();
        byte spins = 0, postSpins = 0;
        // first 我目前是不是排在隊列裡最前面的第一位
        boolean interrupted = false, first = false;
        // predecessor 前一個人
        Node pred = null;

        for (;;) {
            // 執行緒安心呼叫 LockSupport.park() 睡覺之前，做一次嚴密的「前驅有效性與健康度驗證」
            // 防止 信號丟失
            // 排隊前置檢查（過濾前驅異常）
            // 如果我已經在隊列中，但我不是排在最前面的第一位，我得先檢查排在我前面的人正不正常

            if (
                    // 我上一圈是不是第一名
                    !first &&
                    // 我前面有節點嗎?
                    (pred = (node == null) ? null : node.prev) != null &&
                    // 既然前面有人，重新算一次，在我前面的是不是 head
                    !(first = (head == pred))) {

                // 既然你前面還壓著別人、輪不到你搶鎖，你進來之後的第一要務就是「健康檢查」

                // 前面的人棄排了，原因
                // 1. 超時
                // 2. 中斷
                if (pred.status < 0) {
                    // 既然排你前面的人已經棄排，你就不能指望他將來放鎖時叫醒你
                    // 沿著雙向串列往前掃描
                    // 把中途所有 status < 0 的廢棄節點通通拔除
                    // 重新將你前面的指標（prev）對接到更前面一個還健在的節點上
                    cleanQueue();           // predecessor cancelled
                    continue;
                } else if (pred.prev == null) {
                    // 前面的人正在交接拿鎖

                    // 前面的人馬上就要交接好了，我先別動，原地暫停幾個 CPU 時脈週期等他一下

                    Thread.onSpinWait();    // ensure serialization
                    continue;
                }
            }
            // 爭鎖與「出隊扶正」
            // 排在隊頭時的爭鎖嘗試
            // 只有兩種人有資格動手搶鎖：
            // 還沒入隊的新人（pred == null）
            // 以及排在隊伍最前面的第一位（first == true）。
            // 拿到鎖之後，要辦理出隊手續
            // pred == null：代表這條執行緒剛剛進來，連節點都還沒建立入隊。AQS 先給它一次「快篩嘗試」，如果鎖剛好空著，直接拿走，省去入隊開銷。
            // first == true：代表這條執行緒排在雙向佇列的真正第一位（排在 Head 後面）。依照 FIFO 原則，輪到它搶鎖了。
            // AQS 的佇列中，head 永遠代表「當前持有鎖的人（或虛擬哨兵）」。
            if (first || pred == null) {
                boolean acquired;
                try {
                    if (shared)
                        acquired = (tryAcquireShared(arg) >= 0);
                    else
                        // 呼叫子類別嘗試拿鎖
                        acquired = tryAcquire(arg);
                } catch (Throwable ex) {
                    cancelAcquire(node, interrupted, false);
                    throw ex;
                }
                if (acquired) {
                    if (first) {
                        node.prev = null;
                        // 拿到鎖了！出隊，把自己扶正為新的 Head
                        head = node;
                        // 斬斷與舊 Head 的指標鏈結，讓舊 Head 能被 JVM 垃圾回收（GC）
                        pred.next = null;
                        // 將裡面的執行緒指標清空（因為它已經拿到鎖在外面跑了，不再是等待者）。
                        node.waiter = null;
                        if (shared)
                            signalNextIfShared(node);
                        if (interrupted)
                            current.interrupt();
                    }
                    // 宣告通關成功，離開 acquire
                    return 1;
                }
            }
            Node t;
            // 1. 隊列還沒初始化，先建立 Dummy Head
            if ((t = tail) == null) {           // initialize queue
                // 2. 呼叫方法初始化。結果回傳了節點 h，所以 (h == null) 是 false！
                if (tryInitializeHead() == null)
                    return acquireOnOOME(shared, arg);
            // 2. 第一次進來 node 還是 null，才延遲建立 Node
            // 分支 2：延遲建立 Node
                // 只有在真的搶不到鎖、且隊列已經有了，才花記憶體 new 一個節點
            } else if (node == null) {          // allocate; retry before enqueue
                try {
                    node = (shared) ? new SharedNode() : new ExclusiveNode();
                } catch (OutOfMemoryError oome) {
                    return acquireOnOOME(shared, arg);
                }
            // 3. 這才是真正的 addWaiter！
            } else if (pred == null) {          // try to enqueue
                node.waiter = current;
                node.setPrevRelaxed(t);         // avoid unnecessary fence
                // CAS 掛到 tail 後面
                if (!casTail(t, node))
                    node.setPrevRelaxed(null);  // back out
                else
                    // 正式入隊成功！
                    t.next = node;
            // 自旋優化
            } else if (first && spins != 0) {
                --spins;                        // reduce unfairness on rewaits
                // x86 的 PAUSE 指令，讓 CPU 微短暫等待而不耗電
                Thread.onSpinWait();
            // 標記等待狀態（啟用通知）
            } else if (node.status == 0) {
                // 標記為等待中，告訴前任：「你釋放時記得叫醒我」
                node.status = WAITING;          // enable signal and recheck
            } else {
                spins = postSpins = (byte)((postSpins << 1) | 1);
                try {
                    long nanos;
                    if (!timed)
                        // 呼叫 OS 將執行緒徹底休眠，交出 CPU
                        LockSupport.park(this);
                    else if ((nanos = time - System.nanoTime()) > 0L)
                        LockSupport.parkNanos(this, nanos);
                    else
                        break;
                } catch (Error | RuntimeException ex) {
                    cancelAcquire(node, interrupted, interruptible); // cancel & rethrow
                    throw ex;
                }
                node.clearStatus();
                if ((interrupted |= Thread.interrupted()) && interruptible)
                    break;
            }
        }
        return cancelAcquire(node, interrupted, interruptible);
    }

    private Node tryInitializeHead() {
        for (Node h = null, t; ; ) {
            if ((t = tail) != null)
                return t;
            else if (head != null) {
                Thread.onSpinWait();
            } else {
                if (h == null) {
                    try {
                        // 建立一個 Dummy 哨兵節點
                        h = new ExclusiveNode();
                    } catch (OutOfMemoryError oome) {
                        return null;
                    }
                }
                if (U.compareAndSetReference(this, HEAD, null, h))
                    // CAS 成功，head 與 tail 同時指向這個 Dummy 節點
                    // 成功建立，回傳的是新節點 h，不是 null！
                    return tail = h;
            }
        }
    }

    // 當前 state 的值等於 expect 時，才將它修改為 update；
    // 如果成功改掉回傳 true
    // 如果被別人搶先一步改掉就放棄並回傳 false
    protected final boolean compareAndSetState(int expect, int update) {
        return U.compareAndSetInt(this, STATE, expect, update);
    }


    abstract static class Node {
        volatile Node prev;
        volatile Node next;
        Thread waiter;
        // 等待狀態，如 CANCELLED、SIGNAL
        volatile int status;

        final int getAndUnsetStatus(int v) {
            return U.getAndBitwiseAndInt(this, STATUS, ~v);
        }
    }

    private boolean casTail(Node c, Node v) {
        return U.compareAndSetReference(this, TAIL, c, v);
    }

    private static void signalNext(Node h) {
        Node s;
        if (
                // 如果隊列根本沒被初始化過
                // （從頭到尾只有當前這條執行緒在玩，沒有人來排隊爭搶過）
                // head 自然是 null，完全不需要叫人
                h != null &&
                // s == null，代表隊列裡現在除了 Dummy Head 之外根本沒有人在排隊，直接略過
                (s = h.next) != null &&
                // s.status == 0：代表排在後面的那個人才剛掛上隊列，人還清醒著在跑迴圈
                s.status != 0) {
            // 原子操作（CAS），把節點 s 身上的 WAITING 標記清除掉（復位為 0）。
            s.getAndUnsetStatus(WAITING);

            LockSupport.unpark(s.waiter);
        }
    }


}
