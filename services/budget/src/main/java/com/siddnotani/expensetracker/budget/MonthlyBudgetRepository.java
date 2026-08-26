package com.siddnotani.expensetracker.budget;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MonthlyBudgetRepository extends JpaRepository<MonthlyBudget, UUID> {
    @EntityGraph(attributePaths = "category")
    List<MonthlyBudget> findByUserIdAndPeriodOrderByCategory_NameAsc(UUID userId, LocalDate period);

    List<MonthlyBudget> findByUserIdAndPeriod(UUID userId, LocalDate period);

    Optional<MonthlyBudget> findByUserIdAndCategory_IdAndPeriod(
            UUID userId, UUID categoryId, LocalDate period);
}
