package com.example.concurrent.parallel;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

public class PuzzleSolver<P, M> extends ConcurrentPuzzleSolver<P, M>{
    // 1. 維護一個全域的原子計數器，紀錄當前「還在運作中」的 Task 總數
    private final AtomicInteger taskCount = new AtomicInteger(0);

    public PuzzleSolver(Puzzle<P, M> puzzle, ExecutorService exec) {
        super(puzzle, exec);
    }

    // 2. 覆寫 newTask，改傳回具備「計數功能」的 CountingSolverTask
    @Override
    protected Runnable newTask(P p, M m, Node<P, M> n) {
        return new CountingSolverTask(p, m, n);
    }

    class CountingSolverTask extends SolverTask {
        CountingSolverTask(P pos, M move, Node<P, M> n) {
            super(pos, move, n);
            // 每建立一個新 Task，計數器就 +1
            taskCount.incrementAndGet();
        }

        public void run() {
            try {
                super.run();
            } finally {
                // 無論任務是成功、被拋棄還是執行完畢，結束時計數器 -1
                if (taskCount.decrementAndGet() == 0)
                    // 關鍵：如果活躍任務數降為 0，代表「全部搜完了還是沒答案」
                    // 主動填入 null，喚醒卡在 getValue() 的主執行緒！
                    solution.setValue(null);
            }
        }
    }
}
