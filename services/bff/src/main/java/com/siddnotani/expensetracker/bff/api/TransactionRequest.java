package com.siddnotani.expensetracker.bff.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

public record TransactionRequest(
        @NotNull UUID accountId,
        @NotNull String direction,
        @NotNull @Positive Long amountMinor,
        @Pattern(regexp = "[A-Za-z]{3}") String currency,
        @NotNull LocalDate occurredOn,
        @NotBlank @Size(max = 500) String description,
        UUID categoryId) {
}
