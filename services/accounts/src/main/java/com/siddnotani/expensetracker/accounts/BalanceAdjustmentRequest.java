package com.siddnotani.expensetracker.accounts;

import jakarta.validation.constraints.NotNull;

public record BalanceAdjustmentRequest(@NotNull Long deltaMinor) {
}
