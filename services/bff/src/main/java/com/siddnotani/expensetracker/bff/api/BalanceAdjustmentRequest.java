package com.siddnotani.expensetracker.bff.api;

import jakarta.validation.constraints.NotNull;

public record BalanceAdjustmentRequest(@NotNull Long deltaMinor) {
}
