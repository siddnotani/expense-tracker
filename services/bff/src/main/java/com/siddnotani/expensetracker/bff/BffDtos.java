package com.siddnotani.expensetracker.bff;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public final class BffDtos {
    private BffDtos() {
    }

    public record Category(UUID id, String name, String color, boolean archived) {
    }

    public record CreateCategoryRequest(
            @NotBlank @Size(max = 200) String name,
            @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "color must be a hex color")
            String color) {
    }

    public record UpdateCategoryRequest(
            @Size(max = 200) String name,
            @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "color must be a hex color")
            String color,
            Boolean archived) {
    }

    public record Budget(UUID id, UUID categoryId, String categoryName, long amountMinor) {
    }

    public record UpsertBudgetRequest(
            UUID categoryId,
            @NotBlank String period,
            @Min(0) long amountMinor) {
    }

    public record CopyBudgetsRequest(@NotBlank String fromPeriod, @NotBlank String toPeriod) {
    }

    public record TransactionCategoryTotal(UUID categoryId, long totalMinor) {
    }

    public record TransactionSummary(
            long totalExpenseMinor,
            long totalIncomeMinor,
            long netMinor,
            List<TransactionCategoryTotal> byCategory) {
    }

    public record ProgressCategory(
            UUID categoryId,
            String categoryName,
            long budgetedMinor,
            long spentMinor,
            long remainingMinor,
            boolean overBudget) {
    }

    public record BudgetProgress(
            String period,
            long totalBudgetedMinor,
            long totalSpentMinor,
            long remainingMinor,
            List<ProgressCategory> categories) {
    }

    public record Dashboard(
            long totalSpentMinor,
            long totalIncomeMinor,
            long netMinor,
            List<TopCategory> topCategories,
            BudgetProgress budgetProgress) {
    }

    public record TopCategory(UUID categoryId, String categoryName, long spentMinor) {
    }
}
