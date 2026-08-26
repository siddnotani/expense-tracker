package com.siddnotani.expensetracker.transaction;

import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.UUID;

public final class TransactionSpecifications {
    private TransactionSpecifications() {
    }

    public static Specification<Transaction> filtered(
            UUID userId,
            LocalDate from,
            LocalDate to,
            UUID accountId,
            UUID categoryId,
            Direction direction) {
        Specification<Transaction> specification = (root, query, builder) ->
                builder.equal(root.get("userId"), userId);
        if (from != null) {
            specification = specification.and((root, query, builder) ->
                    builder.greaterThanOrEqualTo(root.get("occurredOn"), from));
        }
        if (to != null) {
            specification = specification.and((root, query, builder) ->
                    builder.lessThanOrEqualTo(root.get("occurredOn"), to));
        }
        if (accountId != null) {
            specification = specification.and((root, query, builder) ->
                    builder.equal(root.get("accountId"), accountId));
        }
        if (categoryId != null) {
            specification = specification.and((root, query, builder) ->
                    builder.equal(root.get("categoryId"), categoryId));
        }
        if (direction != null) {
            specification = specification.and((root, query, builder) ->
                    builder.equal(root.get("direction"), direction));
        }
        return specification;
    }
}
