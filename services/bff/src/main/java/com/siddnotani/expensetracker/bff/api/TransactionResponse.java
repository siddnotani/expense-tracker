package com.siddnotani.expensetracker.bff.api;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record TransactionResponse(
        UUID id,
        UUID accountId,
        UUID categoryId,
        String direction,
        long amountMinor,
        String currency,
        LocalDate occurredOn,
        String description,
        OffsetDateTime createdAt) {
}
