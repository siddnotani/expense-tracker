package com.siddnotani.expensetracker.bff.api;

import java.util.UUID;

public class BalanceSyncException extends RuntimeException {
    private final UUID transactionId;
    private final UUID accountId;

    BalanceSyncException(UUID transactionId, UUID accountId, Throwable cause) {
        super("Transaction was recorded but the account balance is stale", cause);
        this.transactionId = transactionId;
        this.accountId = accountId;
    }

    public UUID getTransactionId() {
        return transactionId;
    }

    public UUID getAccountId() {
        return accountId;
    }
}
