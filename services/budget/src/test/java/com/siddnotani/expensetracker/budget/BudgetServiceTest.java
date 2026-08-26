package com.siddnotani.expensetracker.budget;

import com.siddnotani.expensetracker.shared.security.CurrentUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BudgetServiceTest {
    private static final UUID USER_ID = UUID.randomUUID();

    @Mock
    CategoryRepository categoryRepository;
    @Mock
    MonthlyBudgetRepository budgetRepository;
    @Mock
    CurrentUser currentUser;

    private BudgetService service;
    private final PeriodParser periodParser = new PeriodParser();

    @BeforeEach
    void setUp() {
        service = new BudgetService(categoryRepository, budgetRepository, currentUser, periodParser);
    }

    @Test
    void upsertUsesExistingUserCategoryAndPeriod() {
        UUID categoryId = UUID.randomUUID();
        Category category = new Category(USER_ID, "Groceries", null);
        MonthlyBudget existing = new MonthlyBudget(USER_ID, category, YearMonth.of(2025, 1).atDay(1), 1000);
        when(currentUser.getId()).thenReturn(USER_ID);
        when(categoryRepository.findByUserIdAndId(USER_ID, categoryId)).thenReturn(Optional.of(category));
        when(budgetRepository.findByUserIdAndCategory_IdAndPeriod(
                USER_ID, categoryId, YearMonth.of(2025, 1).atDay(1))).thenReturn(Optional.of(existing));
        when(budgetRepository.save(existing)).thenReturn(existing);

        MonthlyBudget result = service.upsertBudget(
                new BudgetDtos.UpsertBudgetRequest(categoryId, "2025-01", 2500));

        assertThat(result).isSameAs(existing);
        assertThat(existing.getAmountMinor()).isEqualTo(2500);
    }

    @Test
    void defaultsAreIdempotent() {
        List<Category> saved = new ArrayList<>();
        when(currentUser.getId()).thenReturn(USER_ID);
        when(categoryRepository.findByUserIdOrderByNameAsc(USER_ID)).thenAnswer(invocation -> saved);
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> {
            Category category = invocation.getArgument(0);
            saved.add(category);
            return category;
        });

        service.createDefaults();
        service.createDefaults();

        assertThat(saved).extracting(Category::getName).containsExactly(
                "Groceries", "Rent", "Transport", "Dining", "Utilities", "Entertainment", "Other");
    }

    @Test
    void malformedPeriodIsRejected() {
        assertThatThrownBy(() -> periodParser.parse("2025-1"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> periodParser.parse("2025-13"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void categoryFromAnotherUserIsNotFound() {
        UUID categoryId = UUID.randomUUID();
        when(currentUser.getId()).thenReturn(USER_ID);
        when(categoryRepository.findByUserIdAndId(USER_ID, categoryId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.upsertBudget(
                new BudgetDtos.UpsertBudgetRequest(categoryId, "2025-01", 100)))
                .isInstanceOf(com.siddnotani.expensetracker.shared.error.NotFoundException.class);
    }
}
