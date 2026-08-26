package com.siddnotani.expensetracker.bff;

import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.siddnotani.expensetracker.bff.BffDtos.Budget;
import static com.siddnotani.expensetracker.bff.BffDtos.BudgetProgress;
import static com.siddnotani.expensetracker.bff.BffDtos.Category;
import static com.siddnotani.expensetracker.bff.BffDtos.Dashboard;
import static com.siddnotani.expensetracker.bff.BffDtos.ProgressCategory;
import static com.siddnotani.expensetracker.bff.BffDtos.TopCategory;
import static com.siddnotani.expensetracker.bff.BffDtos.TransactionCategoryTotal;
import static com.siddnotani.expensetracker.bff.BffDtos.TransactionSummary;

@Service
public class BudgetCompositionService {
    private final BudgetClient budgetClient;
    private final TransactionSummaryClient transactionSummaryClient;
    private final BffPeriodParser periodParser;

    public BudgetCompositionService(
            BudgetClient budgetClient,
            TransactionSummaryClient transactionSummaryClient,
            BffPeriodParser periodParser) {
        this.budgetClient = budgetClient;
        this.transactionSummaryClient = transactionSummaryClient;
        this.periodParser = periodParser;
    }

    public Mono<BudgetProgress> progress(String periodValue) {
        YearMonth period = periodParser.parseOrCurrent(periodValue);
        return fanOut(period).map(result -> composeProgress(period, result.budgets(), result.categories(),
                result.summary()));
    }

    public Mono<Dashboard> dashboard(String periodValue) {
        YearMonth period = periodParser.parseOrCurrent(periodValue);
        return fanOut(period).map(result -> {
            BudgetProgress progress = composeProgress(
                    period, result.budgets(), result.categories(), result.summary());
            List<TopCategory> top = progress.categories().stream()
                    .filter(category -> category.spentMinor() > 0)
                    .sorted(Comparator.comparingLong(ProgressCategory::spentMinor).reversed())
                    .limit(5)
                    .map(category -> new TopCategory(
                            category.categoryId(), category.categoryName(), category.spentMinor()))
                    .toList();
            return new Dashboard(
                    result.summary().totalExpenseMinor(),
                    result.summary().totalIncomeMinor(),
                    result.summary().netMinor(),
                    top,
                    progress);
        });
    }

    private Mono<FanOut> fanOut(YearMonth period) {
        LocalDate first = period.atDay(1);
        LocalDate last = period.atEndOfMonth();
        return Mono.zip(
                        budgetClient.budgets(period.toString()),
                        budgetClient.categories(false),
                        transactionSummaryClient.summary(first, last))
                .map(tuple -> new FanOut(tuple.getT1(), tuple.getT2(), tuple.getT3()));
    }

    private BudgetProgress composeProgress(
            YearMonth period, List<Budget> budgets, List<Category> categories, TransactionSummary summary) {
        Map<UUID, String> names = new HashMap<>();
        categories.forEach(category -> names.put(category.id(), category.name()));
        Map<UUID, Long> budgetAmounts = new HashMap<>();
        budgets.forEach(budget -> {
            names.putIfAbsent(budget.categoryId(), budget.categoryName());
            budgetAmounts.merge(budget.categoryId(), budget.amountMinor(), Math::addExact);
        });
        Map<UUID, Long> spentAmounts = new HashMap<>();
        long nullSpent = 0;
        boolean hasNullCategory = false;
        List<TransactionCategoryTotal> totals =
                summary.byCategory() == null ? List.of() : summary.byCategory();
        for (TransactionCategoryTotal total : totals) {
            if (total.categoryId() == null) {
                hasNullCategory = true;
                nullSpent = Math.addExact(nullSpent, total.totalMinor());
            } else {
                spentAmounts.merge(total.categoryId(), total.totalMinor(), Math::addExact);
            }
        }

        List<ProgressCategory> rows = new ArrayList<>();
        budgetAmounts.keySet().forEach(id -> rows.add(row(id, names.get(id), budgetAmounts, spentAmounts)));
        spentAmounts.keySet().stream()
                .filter(id -> !budgetAmounts.containsKey(id))
                .forEach(id -> rows.add(row(id, names.getOrDefault(id, "Uncategorised"), budgetAmounts, spentAmounts)));
        if (hasNullCategory) {
            rows.add(new ProgressCategory(null, "Uncategorised", 0, nullSpent, -nullSpent, true));
        }
        long totalBudgeted = rows.stream().mapToLong(ProgressCategory::budgetedMinor).sum();
        long totalSpent = rows.stream().mapToLong(ProgressCategory::spentMinor).sum();
        return new BudgetProgress(
                period.toString(), totalBudgeted, totalSpent, totalBudgeted - totalSpent, rows);
    }

    private ProgressCategory row(
            UUID id, String name, Map<UUID, Long> budgets, Map<UUID, Long> spent) {
        long budgeted = budgets.getOrDefault(id, 0L);
        long spentAmount = spent.getOrDefault(id, 0L);
        return new ProgressCategory(
                id, name == null ? "Uncategorised" : name, budgeted, spentAmount,
                budgeted - spentAmount, spentAmount > budgeted);
    }

    private record FanOut(List<Budget> budgets, List<Category> categories, TransactionSummary summary) {
    }
}
