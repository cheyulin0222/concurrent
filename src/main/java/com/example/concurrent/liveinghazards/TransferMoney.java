package com.example.concurrent.liveinghazards;

import lombok.Getter;

public class TransferMoney {

    public static void main(String[] args) {
        TransferMoney service = new TransferMoney();
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

    public void transferMoney(Account fromAccount, Account toAccount, int amount) {
        synchronized (fromAccount) {
            synchronized (toAccount) {
                if (fromAccount.getAmount() < amount) throw new RuntimeException("餘額不足");
                else {
                    fromAccount.out(amount);
                    toAccount.in(amount);
                }
            }
        }
    }

    static class Account {
        private String name;
        @Getter
        private int amount;

        public Account(String name, int amount) {
            this.name = name;
            this.amount = amount;
        }

        public void in(int amount) {
            this.amount += amount;
            System.out.println(name + " 進帳 " + amount + " 元，餘額 " + this.amount + " 元");

        }

        public void out(int amount) {
            this.amount -= amount;
            System.out.println(name + " 出帳 " + amount + " 元，餘額 " + this.amount + " 元");
        }

    }


}
