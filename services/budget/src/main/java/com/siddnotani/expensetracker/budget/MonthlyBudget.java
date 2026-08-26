package com.siddnotani.expensetracker.budget;

import com.siddnotani.expensetracker.shared.money.Money;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "monthly_budgets", schema = "budget")
public class MonthlyBudget {
    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(nullable = false)
    private LocalDate period;

    @Column(name = "amount_minor", nullable = false, precision = 19, scale = 2)
    @Convert(converter = MoneyMinorAttributeConverter.class)
    private BigDecimal amount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected MonthlyBudget() {
    }

    public MonthlyBudget(UUID userId, Category category, LocalDate period, long amountMinor) {
        this.userId = userId;
        this.category = category;
        this.period = period;
        this.amount = Money.fromMinorUnits(amountMinor);
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public Category getCategory() {
        return category;
    }

    public LocalDate getPeriod() {
        return period;
    }

    public long getAmountMinor() {
        return Money.toMinorUnits(amount);
    }

    public void setAmountMinor(long amountMinor) {
        this.amount = Money.fromMinorUnits(amountMinor);
    }
}
