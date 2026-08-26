package com.siddnotani.expensetracker.bff.api;

import java.util.UUID;

public record CategoryTotal(UUID categoryId, long totalMinor) {
}
