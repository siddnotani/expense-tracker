package com.siddnotani.expensetracker.transaction;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record TransactionResponse(
        UUID id,
        UUID accountId,
        UUID categoryId,
        Direction direction,
        long amountMinor,
        String currency,
        LocalDate occurredOn,
        String description,
        OffsetDateTime createdAt) {
    static TransactionResponse from(Transaction transaction) {
        return new TransactionResponse(
                transaction.getId(),
                transaction.getAccountId(),
                transaction.getCategoryId(),
                transaction.getDirection(),
                transaction.getAmountMinor(),
                transaction.getCurrency(),
                transaction.getOccurredOn(),
                transaction.getDescription(),
                transaction.getCreatedAt());
    }
}
