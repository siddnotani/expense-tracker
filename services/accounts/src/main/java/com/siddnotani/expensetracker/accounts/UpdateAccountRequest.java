package com.siddnotani.expensetracker.accounts;

import jakarta.validation.constraints.Size;

public record UpdateAccountRequest(
        @Size(max = 120) String name,
        Boolean archived) {
}
