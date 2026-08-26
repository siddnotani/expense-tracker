package com.siddnotani.expensetracker.accounts;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.OptionalLong;
import java.util.UUID;

@Component
public class AccountBalanceUpdater {
    private static final String UPDATE_BALANCE = """
            UPDATE accounts.accounts SET balance_minor = balance_minor + ?
             WHERE id = ? AND user_id = ? RETURNING balance_minor
            """;

    private final JdbcTemplate jdbcTemplate;

    AccountBalanceUpdater(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public OptionalLong adjust(UUID id, UUID userId, long deltaMinor) {
        try {
            Long balance = jdbcTemplate.queryForObject(
                    UPDATE_BALANCE, Long.class, deltaMinor, id, userId);
            return OptionalLong.of(balance);
        } catch (EmptyResultDataAccessException exception) {
            return OptionalLong.empty();
        }
    }
}
