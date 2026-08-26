package com.siddnotani.expensetracker.bff.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateAccountRequest(
        @NotBlank @Size(max = 120) String name,
        @NotNull String type,
        @Pattern(regexp = "[A-Za-z]{3}") String currency,
        Long openingBalanceMinor) {
}
