package com.siddnotani.expensetracker.bff.api;

import java.util.UUID;

public record BalanceResponse(UUID accountId, long balanceMinor) {
}
