package com.siddnotani.expensetracker.bff;

import reactor.core.publisher.Mono;

import java.time.LocalDate;

public interface TransactionSummaryClient {
    Mono<BffDtos.TransactionSummary> summary(LocalDate from, LocalDate to);
}
