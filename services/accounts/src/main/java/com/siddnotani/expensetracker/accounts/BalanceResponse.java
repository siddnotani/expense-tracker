package com.siddnotani.expensetracker.accounts;

import java.util.UUID;

public record BalanceResponse(UUID accountId, long balanceMinor) {
}
