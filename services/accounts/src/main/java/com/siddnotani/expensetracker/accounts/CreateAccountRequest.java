package com.siddnotani.expensetracker.accounts;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateAccountRequest(
        @NotBlank @Size(max = 120) String name,
        @NotNull AccountType type,
        @Pattern(regexp = "[A-Za-z]{3}") String currency,
        Long openingBalanceMinor) {
    public CreateAccountRequest {
        currency = currency == null ? "USD" : currency.toUpperCase();
        openingBalanceMinor = openingBalanceMinor == null ? 0L : openingBalanceMinor;
    }
}
