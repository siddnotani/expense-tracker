package com.siddnotani.expensetracker.accounts;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AccountResponse(
        UUID id,
        String name,
        AccountType type,
        String currency,
        long balanceMinor,
        boolean archived,
        OffsetDateTime createdAt) {
    static AccountResponse from(Account account) {
        return new AccountResponse(
                account.getId(),
                account.getName(),
                account.getType(),
                account.getCurrency(),
                account.getBalanceMinor(),
                account.isArchived(),
                account.getCreatedAt());
    }
}
