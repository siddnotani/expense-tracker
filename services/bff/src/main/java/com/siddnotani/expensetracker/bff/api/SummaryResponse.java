package com.siddnotani.expensetracker.bff.api;

import java.util.List;

public record SummaryResponse(
        long totalExpenseMinor,
        long totalIncomeMinor,
        long netMinor,
        List<CategoryTotal> byCategory) {
}
