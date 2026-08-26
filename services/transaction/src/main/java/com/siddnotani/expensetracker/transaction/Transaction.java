package com.siddnotani.expensetracker.transaction;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "transactions", schema = "txn")
public class Transaction {
    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Column(name = "category_id")
    private UUID categoryId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Direction direction;

    @Column(name = "amount_minor", nullable = false)
    private long amountMinor;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "occurred_on", nullable = false)
    private LocalDate occurredOn;

    @Column(nullable = false, length = 500)
    private String description;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected Transaction() {
    }

    public Transaction(
            UUID userId,
            UUID accountId,
            UUID categoryId,
            Direction direction,
            long amountMinor,
            String currency,
            LocalDate occurredOn,
            String description) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.accountId = accountId;
        this.categoryId = categoryId;
        this.direction = direction;
        this.amountMinor = amountMinor;
        this.currency = currency;
        this.occurredOn = occurredOn;
        this.description = description;
        this.createdAt = OffsetDateTime.now();
    }

    @PrePersist
    void initializeDefaults() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getAccountId() {
        return accountId;
    }

    public UUID getCategoryId() {
        return categoryId;
    }

    public Direction getDirection() {
        return direction;
    }

    public long getAmountMinor() {
        return amountMinor;
    }

    public String getCurrency() {
        return currency;
    }

    public LocalDate getOccurredOn() {
        return occurredOn;
    }

    public String getDescription() {
        return description;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    void update(
            UUID accountId,
            UUID categoryId,
            Direction direction,
            long amountMinor,
            String currency,
            LocalDate occurredOn,
            String description) {
        this.accountId = accountId;
        this.categoryId = categoryId;
        this.direction = direction;
        this.amountMinor = amountMinor;
        this.currency = currency;
        this.occurredOn = occurredOn;
        this.description = description;
    }
}
