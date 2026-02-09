package com.nepalnationalteam.blackjackdesktop;

public class UserProfile {
    private final String id;
    private final String displayName;
    private int wallet;

    public UserProfile(String id, String displayName, int wallet) {
        this.id = id;
        this.displayName = displayName;
        this.wallet = wallet;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getWallet() {
        return wallet;
    }

    public void setWallet(int wallet) {
        this.wallet = wallet;
    }
}
