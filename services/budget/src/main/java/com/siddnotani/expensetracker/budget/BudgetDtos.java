package com.siddnotani.expensetracker.budget;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public final class BudgetDtos {
    private BudgetDtos() {
    }

    public record CategoryResponse(UUID id, String name, String color, boolean archived) {
        static CategoryResponse from(Category category) {
            return new CategoryResponse(
                    category.getId(), category.getName(), category.getColor(), category.isArchived());
        }
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

    public record BudgetResponse(UUID id, UUID categoryId, String categoryName, long amountMinor) {
        static BudgetResponse from(MonthlyBudget budget) {
            return new BudgetResponse(
                    budget.getId(),
                    budget.getCategory().getId(),
                    budget.getCategory().getName(),
                    budget.getAmountMinor());
        }
    }

    public record UpsertBudgetRequest(
            UUID categoryId,
            @NotBlank String period,
            @Min(0) long amountMinor) {
    }

    public record CopyBudgetsRequest(@NotBlank String fromPeriod, @NotBlank String toPeriod) {
    }
}
