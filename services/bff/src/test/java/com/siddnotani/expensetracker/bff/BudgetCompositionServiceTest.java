package com.siddnotani.expensetracker.bff;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BudgetCompositionServiceTest {
    @Mock
    BudgetClient budgetClient;
    @Mock
    TransactionSummaryClient summaryClient;

    @Test
    void progressIncludesUnbudgetedAndUncategorisedSpending() {
        UUID groceries = UUID.randomUUID();
        UUID unbudgeted = UUID.randomUUID();
        when(budgetClient.budgets("2025-01")).thenReturn(Mono.just(List.of(
                new BffDtos.Budget(UUID.randomUUID(), groceries, "Groceries", 10000))));
        when(budgetClient.categories(false)).thenReturn(Mono.just(List.of(
                new BffDtos.Category(groceries, "Groceries", null, false),
                new BffDtos.Category(unbudgeted, "Travel", null, false))));
        when(summaryClient.summary(
                java.time.LocalDate.of(2025, 1, 1), java.time.LocalDate.of(2025, 1, 31)))
                .thenReturn(Mono.just(new BffDtos.TransactionSummary(
                        13000, 0, -13000, List.of(
                        new BffDtos.TransactionCategoryTotal(groceries, 12000),
                        new BffDtos.TransactionCategoryTotal(unbudgeted, 500),
                        new BffDtos.TransactionCategoryTotal(null, 500)))));

        BudgetCompositionService service = new BudgetCompositionService(
                budgetClient, summaryClient, new BffPeriodParser());

        BffDtos.BudgetProgress result = service.progress("2025-01").block();

        assertThat(result.totalBudgetedMinor()).isEqualTo(10000);
        assertThat(result.totalSpentMinor()).isEqualTo(13000);
        assertThat(result.categories()).anyMatch(row ->
                row.categoryName().equals("Groceries") && row.overBudget());
        assertThat(result.categories()).anyMatch(row ->
                row.categoryName().equals("Travel") && row.budgetedMinor() == 0);
        assertThat(result.categories()).anyMatch(row ->
                row.categoryId() == null && row.categoryName().equals("Uncategorised"));
    }
}
