package com.siddnotani.expensetracker.transaction;

import java.util.UUID;

public record CategoryTotal(UUID categoryId, long totalMinor) {
}
