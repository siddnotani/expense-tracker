package com.siddnotani.expensetracker.accounts;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalLong;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Testcontainers
class AccountRepositoryTest {
    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    AccountRepository accountRepository;

    @Autowired
    AccountBalanceUpdater balanceUpdater;

    @AfterEach
    void clean() {
        accountRepository.deleteAll();
    }

    @Test
    void enforcesCaseInsensitiveNamePerUser() {
        UUID user = UUID.randomUUID();
        accountRepository.saveAndFlush(new Account(user, "Cash", AccountType.CASH, "USD", 0));

        assertThrows(
                DataIntegrityViolationException.class,
                () -> accountRepository.saveAndFlush(
                        new Account(user, "cash", AccountType.CASH, "USD", 0)));

        Account otherUser = accountRepository.saveAndFlush(
                new Account(UUID.randomUUID(), "cash", AccountType.CASH, "USD", 0));
        assertEquals("cash", otherUser.getName());
    }

    @Test
    void atomicallyAppliesConcurrentBalanceAdjustments() throws Exception {
        UUID user = UUID.randomUUID();
        Account account = accountRepository.saveAndFlush(
                new Account(user, "Cash", AccountType.CASH, "USD", 0));
        int threads = 8;
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        List<Future<OptionalLong>> futures = new ArrayList<>();
        try {
            for (int i = 0; i < threads; i++) {
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    start.await();
                    return balanceUpdater.adjust(account.getId(), user, 100);
                }));
            }
            ready.await();
            start.countDown();
            for (Future<OptionalLong> future : futures) {
                assertTrue(future.get().isPresent());
            }
        } finally {
            executor.shutdownNow();
        }
        assertEquals(threads * 100, accountRepository.findById(account.getId()).orElseThrow()
                .getBalanceMinor());
    }

    @Test
    void doesNotFindAnotherUsersAccount() {
        UUID owner = UUID.randomUUID();
        Account account = accountRepository.saveAndFlush(
                new Account(owner, "Cash", AccountType.CASH, "USD", 0));

        assertFalse(accountRepository.findByIdAndUserId(account.getId(), UUID.randomUUID()).isPresent());
    }
}
