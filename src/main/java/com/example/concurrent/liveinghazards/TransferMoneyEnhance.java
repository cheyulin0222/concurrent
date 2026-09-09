package com.example.concurrent.liveinghazards;

import lombok.Getter;

public class TransferMoneyEnhance {

    private static final Object tieLock = new Object();

    public static void main(String[] args) {
        TransferMoneyEnhance service = new TransferMoneyEnhance();
        Account mary = new Account("Mary", 100000);
        Account john = new Account("John", 100000);

        Thread t1 = new Thread(() -> {
            while (true) {
                service.transferMoney(mary, john, 1);
            }
        }, "Thread-Mary-To-John");

        Thread t2 = new Thread(() -> {
            while (true) {
                service.transferMoney(john, mary, 1);
            }
        }, "Thread-John-To-Mary");

        t1.start();
        t2.start();

    }

    public void transferMoney(Account from, Account to, int amount) {
        class Helper {
            public void transfer() {
                if (from.getBalance() < amount) throw new RuntimeException("餘額不足");
                else {
                    from.out(amount);
                    to.in(amount);
                }
            }
        }

        int fromHash = System.identityHashCode(from);
        int toHash = System.identityHashCode(to);

        if (fromHash < toHash) {
            synchronized (from) {
                synchronized (to) {
                    new Helper().transfer();
                }
            }
        } else if (fromHash > toHash) {
            synchronized (to) {
                synchronized (from) {
                    new Helper().transfer();
                }
            }
        } else {
            synchronized (tieLock) {
                synchronized (from) {
                    synchronized (to) {
                        new Helper().transfer();
                    }
                }
            }
        }
    }

    static class Account {
        private String name;
        @Getter
        private int balance;

        public Account(String name, int balance) {
            this.name = name;
            this.balance = balance;
        }

        public void in(int amount) {
            this.balance += amount;

            System.out.println(name + " 進帳 " + amount + " 元，餘額 " + this.balance + " 元");

        }

        public void out(int amount) {
            this.balance -= amount;
            System.out.println(name + " 出帳 " + amount + " 元，餘額 " + this.balance + " 元");

        }

    }
}
