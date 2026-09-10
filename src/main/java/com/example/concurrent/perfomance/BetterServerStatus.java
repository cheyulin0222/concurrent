package com.example.concurrent.perfomance;

import java.util.Set;

public class BetterServerStatus {

    public final Set<String> users;
    public final Set<String> queries;

    public void addUser(String u) {
        synchronized (users) {
            users.add(u);
        }
    }

    public void addQuery(String q) {
        synchronized (queries) {
            queries.add(q);
        }
    }
}
