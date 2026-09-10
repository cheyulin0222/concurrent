package com.example.concurrent.perfomance;

public class UserProfile {
    // 欄位全部為 final，建立後無法修改
    private final String userId;
    private final String email;
    private final int level;

    public UserProfile(String userId, String email, int level) {
        this.userId = userId;
        this.email = email;
        this.level = level;
    }

    public String getUserId() {
        return userId;
    }

    public String getEmail() {
        return email;
    }

    public int getLevel() {
        return level;
    }

    // 若需要「修改」，不是改原物件，而是複製一份傳回「新物件」
    public UserProfile upgradeLevel() {
        return new UserProfile(this.userId, this.email, this.level + 1);
    }

}
