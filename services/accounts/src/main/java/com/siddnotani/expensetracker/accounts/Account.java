package com.siddnotani.expensetracker.accounts;

import com.siddnotani.expensetracker.shared.money.Money;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "accounts", schema = "accounts")
public class Account {
    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccountType type;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "balance_minor", nullable = false)
    private long balanceMinor;

    @Column(nullable = false)
    private boolean archived;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected Account() {
    }

    public Account(UUID userId, String name, AccountType type, String currency, long balanceMinor) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.name = name;
        this.type = type;
        this.currency = currency;
        this.balanceMinor = balanceMinor;
        this.archived = false;
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

    public String getName() {
        return name;
    }

    public AccountType getType() {
        return type;
    }

    public String getCurrency() {
        return currency;
    }

    public long getBalanceMinor() {
        return balanceMinor;
    }

    public BigDecimal balance() {
        return Money.fromMinorUnits(balanceMinor);
    }

    public boolean isArchived() {
        return archived;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    void setName(String name) {
        this.name = name;
    }

    void setArchived(boolean archived) {
        this.archived = archived;
    }
}
