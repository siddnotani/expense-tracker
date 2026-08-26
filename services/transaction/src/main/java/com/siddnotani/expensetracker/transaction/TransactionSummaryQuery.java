package com.siddnotani.expensetracker.transaction;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class TransactionSummaryQuery {
    private final JdbcTemplate jdbcTemplate;

    TransactionSummaryQuery(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public SummaryResponse summarize(UUID userId, LocalDate from, LocalDate to) {
        String dateWhere = dateWhere(from, to);
        List<Object> parameters = new ArrayList<>();
        parameters.add(userId);
        addDateParameters(parameters, from, to);
        String totalsSql = """
                SELECT
                  COALESCE(SUM(CASE WHEN direction = 'EXPENSE' THEN amount_minor ELSE 0 END), 0),
                  COALESCE(SUM(CASE WHEN direction = 'INCOME' THEN amount_minor ELSE 0 END), 0)
                FROM txn.transactions
                WHERE user_id = ?%s
                """.formatted(dateWhere);
        long[] totals = jdbcTemplate.queryForObject(totalsSql, (resultSet, rowNum) ->
                new long[] {resultSet.getLong(1), resultSet.getLong(2)}, parameters.toArray());

        String categorySql = """
                SELECT category_id,
                       COALESCE(SUM(amount_minor), 0) AS total_minor
                FROM txn.transactions
                WHERE user_id = ? AND direction = 'EXPENSE'%s
                GROUP BY category_id
                ORDER BY total_minor DESC
                """.formatted(dateWhere);
        List<CategoryTotal> categories = jdbcTemplate.query(
                categorySql,
                (resultSet, rowNum) -> new CategoryTotal(
                        resultSet.getObject(1, UUID.class), resultSet.getLong(2)),
                parameters.toArray());
        return new SummaryResponse(totals[0], totals[1], totals[1] - totals[0], categories);
    }

    private String dateWhere(LocalDate from, LocalDate to) {
        StringBuilder where = new StringBuilder();
        if (from != null) {
            where.append(" AND occurred_on >= ?");
        }
        if (to != null) {
            where.append(" AND occurred_on <= ?");
        }
        return where.toString();
    }

    private void addDateParameters(List<Object> parameters, LocalDate from, LocalDate to) {
        if (from != null) {
            parameters.add(Date.valueOf(from));
        }
        if (to != null) {
            parameters.add(Date.valueOf(to));
        }
    }
}
