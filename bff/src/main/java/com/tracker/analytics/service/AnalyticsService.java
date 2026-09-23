package com.tracker.analytics.service;

import com.tracker.analytics.dto.AnalyticsSummaryResponse;
import com.tracker.analytics.dto.CategorySpend;
import com.tracker.analytics.dto.MonthlySpend;
import com.tracker.analytics.dto.SummaryStats;
import com.tracker.expense.repository.ExpenseRepository;
import com.tracker.expense.repository.projection.SpendStats;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnalyticsService {

    private static final int MONEY_SCALE = 2;
    private static final int PERCENT_SCALE = 1;
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final ExpenseRepository expenseRepository;

    public AnalyticsService(ExpenseRepository expenseRepository) {
        this.expenseRepository = expenseRepository;
    }

    @Transactional(readOnly = true)
    public AnalyticsSummaryResponse summary(LocalDate from, LocalDate to) {
        SpendStats stats = expenseRepository.stats(from, to);
        long count = stats == null ? 0 : stats.getCount();
        if (count == 0) {
            return new AnalyticsSummaryResponse(
                    money(BigDecimal.ZERO),
                    new SummaryStats(0, money(BigDecimal.ZERO), money(BigDecimal.ZERO), money(BigDecimal.ZERO)),
                    List.of(),
                    List.of());
        }

        BigDecimal total = money(stats.getTotal());
        BigDecimal average = stats.getTotal().divide(BigDecimal.valueOf(count), MONEY_SCALE, RoundingMode.HALF_UP);

        List<CategorySpend> byCategory = expenseRepository.sumByCategory(from, to).stream()
                .map(row -> new CategorySpend(
                        row.getCategory(),
                        row.getDisplayName(),
                        money(row.getTotal()),
                        percentage(row.getTotal(), total)))
                .toList();

        List<MonthlySpend> byMonth = expenseRepository.sumByMonth(from, to).stream()
                .map(row -> new MonthlySpend(
                        String.format("%04d-%02d", row.getYear(), row.getMonth()),
                        money(row.getTotal())))
                .toList();

        return new AnalyticsSummaryResponse(
                total,
                new SummaryStats(count, average, money(stats.getMin()), money(stats.getMax())),
                byCategory,
                byMonth);
    }

    private static BigDecimal money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    private static BigDecimal percentage(BigDecimal part, BigDecimal total) {
        if (total.signum() == 0) {
            return BigDecimal.ZERO.setScale(PERCENT_SCALE, RoundingMode.HALF_UP);
        }
        return part.multiply(HUNDRED).divide(total, PERCENT_SCALE, RoundingMode.HALF_UP);
    }
}
