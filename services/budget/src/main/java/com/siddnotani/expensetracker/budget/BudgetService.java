package com.siddnotani.expensetracker.budget;

import com.siddnotani.expensetracker.shared.error.NotFoundException;
import com.siddnotani.expensetracker.shared.security.CurrentUser;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class BudgetService {
    private static final List<String> DEFAULT_NAMES = List.of(
            "Groceries", "Rent", "Transport", "Dining", "Utilities", "Entertainment", "Other");

    private final CategoryRepository categoryRepository;
    private final MonthlyBudgetRepository budgetRepository;
    private final CurrentUser currentUser;
    private final PeriodParser periodParser;

    public BudgetService(
            CategoryRepository categoryRepository,
            MonthlyBudgetRepository budgetRepository,
            CurrentUser currentUser,
            PeriodParser periodParser) {
        this.categoryRepository = categoryRepository;
        this.budgetRepository = budgetRepository;
        this.currentUser = currentUser;
        this.periodParser = periodParser;
    }

    public List<Category> categories(boolean includeArchived) {
        return categoryRepository.findByUserIdOrderByNameAsc(currentUser.getId()).stream()
                .filter(category -> includeArchived || !category.isArchived())
                .toList();
    }

    @Transactional
    public List<Category> createDefaults() {
        UUID userId = currentUser.getId();
        Set<String> existing = categoryRepository.findByUserIdOrderByNameAsc(userId).stream()
                .map(category -> category.getName().toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());
        DEFAULT_NAMES.stream()
                .filter(name -> !existing.contains(name.toLowerCase(Locale.ROOT)))
                .map(name -> new Category(userId, name, null))
                .forEach(categoryRepository::save);
        return categories(true);
    }

    @Transactional
    public Category createCategory(BudgetDtos.CreateCategoryRequest request) {
        UUID userId = currentUser.getId();
        return categoryRepository.saveAndFlush(new Category(userId, request.name().trim(), request.color()));
    }

    @Transactional
    public Category updateCategory(UUID id, BudgetDtos.UpdateCategoryRequest request) {
        Category category = categoryRepository.findByUserIdAndId(currentUser.getId(), id)
                .orElseThrow(() -> new NotFoundException("Category not found"));
        if (request.name() == null && request.color() == null && request.archived() == null) {
            return category;
        }
        category.update(
                request.name() == null ? null : request.name().trim(),
                request.color(),
                request.archived());
        return categoryRepository.save(category);
    }

    @Transactional
    public void archiveCategory(UUID id) {
        Category category = categoryRepository.findByUserIdAndId(currentUser.getId(), id)
                .orElseThrow(() -> new NotFoundException("Category not found"));
        category.update(null, null, true);
        categoryRepository.save(category);
    }

    public List<MonthlyBudget> budgets(String period) {
        LocalDate month = periodParser.parse(period).atDay(1);
        return budgetRepository.findByUserIdAndPeriodOrderByCategory_NameAsc(currentUser.getId(), month);
    }

    @Transactional
    public MonthlyBudget upsertBudget(BudgetDtos.UpsertBudgetRequest request) {
        UUID userId = currentUser.getId();
        Category category = categoryRepository.findByUserIdAndId(userId, request.categoryId())
                .orElseThrow(() -> new NotFoundException("Category not found"));
        LocalDate period = periodParser.parse(request.period()).atDay(1);
        MonthlyBudget budget = budgetRepository
                .findByUserIdAndCategory_IdAndPeriod(userId, request.categoryId(), period)
                .orElseGet(() -> new MonthlyBudget(userId, category, period, request.amountMinor()));
        budget.setAmountMinor(request.amountMinor());
        return budgetRepository.save(budget);
    }

    @Transactional
    public void deleteBudget(UUID id) {
        MonthlyBudget budget = budgetRepository.findById(id)
                .filter(candidate -> candidate.getUserId().equals(currentUser.getId()))
                .orElseThrow(() -> new NotFoundException("Budget not found"));
        budgetRepository.delete(budget);
    }

    @Transactional
    public int copyBudgets(String fromPeriod, String toPeriod) {
        UUID userId = currentUser.getId();
        LocalDate from = periodParser.parse(fromPeriod).atDay(1);
        LocalDate to = periodParser.parse(toPeriod).atDay(1);
        Set<UUID> alreadyBudgeted = budgetRepository.findByUserIdAndPeriod(userId, to).stream()
                .map(budget -> budget.getCategory().getId())
                .collect(Collectors.toSet());
        List<MonthlyBudget> copies = budgetRepository.findByUserIdAndPeriod(userId, from).stream()
                .filter(budget -> !alreadyBudgeted.contains(budget.getCategory().getId()))
                .map(budget -> new MonthlyBudget(
                        userId, budget.getCategory(), to, budget.getAmountMinor()))
                .toList();
        budgetRepository.saveAll(copies);
        return copies.size();
    }
}
