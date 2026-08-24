package com.example.concurrent.parallel;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

public class ConcurrentPuzzleSolver<P, M> {
    private final Puzzle<P, M> puzzle;
    private final ExecutorService exec;
    private volatile ConcurrentHashMap<P, Boolean> seen = new ConcurrentHashMap<>();
    final ValueLatch<Node<P, M>> solution = new ValueLatch<>();

    public ConcurrentPuzzleSolver(Puzzle<P, M> puzzle, ExecutorService exec) {
        this.exec = exec;
        this.puzzle = puzzle;
    }

    public List<M> solve() throws InterruptedException {
        try {
            // 產生初始點
            P pos = puzzle.initialPosition();
            // 產生 Task 處理每個點
            exec.execute(newTask(pos, null, null));
            Node<P, M> value = solution.getValue();
            return value == null ? null : value.asMoveList();
        } finally {
            exec.shutdown();
        }
    }

    protected Runnable newTask(P pos, M move, Node<P, M> prev) {
        return new SolverTask(pos, move, prev);
    }

    class SolverTask extends Node<P, M> implements Runnable {
        SolverTask(P pos, M move, Node<P, M> n) {
            super(pos, move, n);
        }
        @Override
        public void run() {
            if (!solution.isSet() || seen.putIfAbsent(pos, true) != null)
                return;
            if (puzzle.isGoal(pos))
                solution.setValue(this);
            else
                for (M m : puzzle.legalMoves(pos))
                    exec.execute(newTask(puzzle.move(pos, m), m, this));
        }
    }


    static class Node<P, M> {
        final P pos;
        final M move;
        final Node<P, M> prev;

        Node(P pos, M move, Node<P, M> prev) {
            this.pos = pos;
            this.move = move;
            this.prev = prev;
        }

        List<M> asMoveList() {
            List<M> solution = new LinkedList<>();
            for (Node<P, M> n = this; n.move != null; n = n.prev)
                solution.add(0, n.move);
            return solution;
        }
    }

}
