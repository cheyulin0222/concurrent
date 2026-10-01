package com.example.concurrent.perfomance;

import java.util.Set;

// 雖然拆分了，但是四個方法都是共用同一把鎖
public class ServerStatus {
    public final Set<String> users;
    public final Set<String> queries;

    public ServerStatus(Set<String> users, Set<String> queries) {
        this.users = users;
        this.queries = queries;
    }

    public synchronized void addUser(String u) {
        users.add(u);
    }

    // 和上面是一樣的意思
//    public void addUser(String u) {
//        synchronized (this) {
//            users.add(u);
//        }
//    }

    public synchronized void addQuery(String q) {
        queries.add(q);
    }

    public synchronized void removeUser(String u) {
        users.remove(u);
    }

    public synchronized void removeQuery(String q) {
        queries.remove(q);
    }
}
