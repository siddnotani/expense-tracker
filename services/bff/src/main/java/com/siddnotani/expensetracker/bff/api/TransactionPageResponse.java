package com.siddnotani.expensetracker.bff.api;

import java.util.List;

public record TransactionPageResponse(
        List<TransactionResponse> items,
        long total,
        int page,
        int size) {
}
