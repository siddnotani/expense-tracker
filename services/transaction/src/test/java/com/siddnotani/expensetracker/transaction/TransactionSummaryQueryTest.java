package com.siddnotani.expensetracker.transaction;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@Testcontainers
class TransactionSummaryQueryTest {
    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    TransactionRepository transactionRepository;

    @Autowired
    TransactionSummaryQuery summaryQuery;

    @AfterEach
    void clean() {
        transactionRepository.deleteAll();
    }

    @Test
    void summarizesDirectionsCategoriesAndDateWindow() {
        UUID user = UUID.randomUUID();
        UUID category = UUID.randomUUID();
        transactionRepository.saveAllAndFlush(List.of(
                tx(user, category, Direction.EXPENSE, 100, "2024-01-01"),
                tx(user, category, Direction.INCOME, 250, "2024-01-02"),
                tx(user, null, Direction.EXPENSE, 30, "2024-01-03"),
                tx(user, UUID.randomUUID(), Direction.EXPENSE, 999, "2024-02-01")));

        SummaryResponse summary = summaryQuery.summarize(
                user, LocalDate.parse("2024-01-01"), LocalDate.parse("2024-01-31"));
        assertEquals(130, summary.totalExpenseMinor());
        assertEquals(250, summary.totalIncomeMinor());
        assertEquals(120, summary.netMinor());
        assertEquals(2, summary.byCategory().size());
        assertEquals(100, summary.byCategory().stream()
                .filter(value -> category.equals(value.categoryId())).findFirst().orElseThrow()
                .totalMinor());
        assertEquals(30, summary.byCategory().stream()
                .filter(value -> value.categoryId() == null).findFirst().orElseThrow()
                .totalMinor());
    }

    private Transaction tx(UUID user, UUID category, Direction direction, long amount, String date) {
        return new Transaction(user, UUID.randomUUID(), category, direction, amount, "USD",
                LocalDate.parse(date), "transaction");
    }
}
