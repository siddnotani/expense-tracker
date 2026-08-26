package com.siddnotani.expensetracker.bff.api;

import jakarta.validation.constraints.Size;

public record UpdateAccountRequest(@Size(max = 120) String name, Boolean archived) {
}
