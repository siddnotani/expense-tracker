package com.siddnotani.expensetracker.bff.api;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AccountResponse(
        UUID id,
        String name,
        String type,
        String currency,
        long balanceMinor,
        boolean archived,
        OffsetDateTime createdAt) {
}
