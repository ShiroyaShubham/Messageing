package com.adsdk.plugin;

public class RewardEarned {
    private int amount;
    private String type = "";

    public int getAmount() {
        return amount;
    }

    protected void setAmount(int amount) {
        this.amount = amount;
    }

    public String getType() {
        return type;
    }

    protected void setType(String type) {
        this.type = type;
    }
}
