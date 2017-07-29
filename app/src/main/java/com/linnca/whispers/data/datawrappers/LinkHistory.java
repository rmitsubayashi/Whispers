package com.linnca.whispers.data.datawrappers;

public class LinkHistory {
    private String transactionDateTime;
    private long amount;
    private int transactionType;
    public static final int TRANSACTION_TYPE_SUCCESSFUL_CHAIN = 0;
    public static final int TRANSACTION_TYPE_TEACH = 1;
    public static final int TRANSACTION_TYPE_LEARN = 2;
    public static final int TRANSACTION_TYPE_CANCEL_LEARN = 3;
    public static final int TRANSACTION_TYPE_PURCHASE = 4;
    public static final int TRANSACTION_TYPE_LOGIN_REWARD = 5;

    public LinkHistory(){}

    public LinkHistory(String transactionDateTime, long amount, int transactionType) {
        this.transactionDateTime = transactionDateTime;
        this.amount = amount;
        this.transactionType = transactionType;
    }

    public String getTransactionDateTime() {
        return transactionDateTime;
    }

    public void setTransactionDateTime(String transactionDateTime) {
        this.transactionDateTime = transactionDateTime;
    }

    public long getAmount() {
        return amount;
    }

    public void setAmount(long amount) {
        this.amount = amount;
    }

    public int getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(int transactionType) {
        this.transactionType = transactionType;
    }
}
