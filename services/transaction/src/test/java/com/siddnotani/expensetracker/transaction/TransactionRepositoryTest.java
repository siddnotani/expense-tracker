package com.siddnotani.expensetracker.transaction;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Testcontainers
class TransactionRepositoryTest {
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

    @AfterEach
    void clean() {
        transactionRepository.deleteAll();
    }

    @Test
    void enforcesPositiveAmountAtDatabaseLevel() {
        UUID user = UUID.randomUUID();
        assertThrows(DataIntegrityViolationException.class, () -> transactionRepository.saveAndFlush(
                new Transaction(user, UUID.randomUUID(), null, Direction.EXPENSE, 0,
                        "USD", LocalDate.now(), "zero")));
        assertThrows(DataIntegrityViolationException.class, () -> transactionRepository.saveAndFlush(
                new Transaction(user, UUID.randomUUID(), null, Direction.EXPENSE, -1,
                        "USD", LocalDate.now(), "negative")));
    }

    @Test
    void filtersInclusivelyAndByAllOptionalFields() {
        UUID user = UUID.randomUUID();
        UUID account = UUID.randomUUID();
        UUID category = UUID.randomUUID();
        transactionRepository.saveAllAndFlush(List.of(
                transaction(user, account, category, Direction.EXPENSE, 10, "2024-01-01"),
                transaction(user, account, null, Direction.INCOME, 20, "2024-01-02"),
                transaction(user, UUID.randomUUID(), category, Direction.EXPENSE, 30, "2024-01-03"),
                transaction(UUID.randomUUID(), account, category, Direction.EXPENSE, 40, "2024-01-02")));

        Page<Transaction> result = transactionRepository.findAll(
                TransactionSpecifications.filtered(user, LocalDate.parse("2024-01-01"),
                        LocalDate.parse("2024-01-03"), account, category, Direction.EXPENSE),
                PageRequest.of(0, 50));
        assertEquals(1, result.getTotalElements());
        assertEquals(10, result.getContent().getFirst().getAmountMinor());
    }

    @Test
    void paginatesNewestFirstAndFindsOnlyOwnedRows() {
        UUID user = UUID.randomUUID();
        for (int i = 0; i < 55; i++) {
            transactionRepository.save(new Transaction(user, UUID.randomUUID(), null,
                    Direction.EXPENSE, i + 1, "USD", LocalDate.of(2024, 1, 1).plusDays(i),
                    "transaction " + i));
        }
        transactionRepository.flush();
        Sort sort = Sort.by(Sort.Order.desc("occurredOn"), Sort.Order.desc("createdAt"));
        Page<Transaction> first = transactionRepository.findAll(
                TransactionSpecifications.filtered(user, null, null, null, null, null),
                PageRequest.of(0, 50, sort));
        Page<Transaction> second = transactionRepository.findAll(
                TransactionSpecifications.filtered(user, null, null, null, null, null),
                PageRequest.of(1, 50, sort));
        assertEquals(55, first.getTotalElements());
        assertEquals(50, first.getNumberOfElements());
        assertEquals(5, second.getNumberOfElements());
        assertEquals(55, first.getContent().getFirst().getAmountMinor());
        assertFalse(transactionRepository.findByIdAndUserId(
                first.getContent().getFirst().getId(), UUID.randomUUID()).isPresent());
    }

    private Transaction transaction(
            UUID user, UUID account, UUID category, Direction direction, long amount, String date) {
        return new Transaction(user, account, category, direction, amount, "USD",
                LocalDate.parse(date), "transaction");
    }
}
