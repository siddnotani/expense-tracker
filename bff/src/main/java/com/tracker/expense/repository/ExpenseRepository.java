package com.tracker.expense.repository;

import com.tracker.expense.model.Expense;
import com.tracker.expense.repository.projection.CategoryTotal;
import com.tracker.expense.repository.projection.MonthTotal;
import com.tracker.expense.repository.projection.SpendStats;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    @Query("""
            SELECT COUNT(e) AS count, COALESCE(SUM(e.amount), 0) AS total,
                   MIN(e.amount) AS min, MAX(e.amount) AS max
            FROM Expense e
            WHERE (:from IS NULL OR e.date >= :from) AND (:to IS NULL OR e.date <= :to)
            """)
    SpendStats stats(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("""
            SELECT c.name AS category, c.displayName AS displayName, SUM(e.amount) AS total
            FROM Expense e JOIN e.category c
            WHERE (:from IS NULL OR e.date >= :from) AND (:to IS NULL OR e.date <= :to)
            GROUP BY c.name, c.displayName
            ORDER BY SUM(e.amount) DESC, c.name ASC
            """)
    List<CategoryTotal> sumByCategory(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("""
            SELECT YEAR(e.date) AS year, MONTH(e.date) AS month, SUM(e.amount) AS total
            FROM Expense e
            WHERE (:from IS NULL OR e.date >= :from) AND (:to IS NULL OR e.date <= :to)
            GROUP BY YEAR(e.date), MONTH(e.date)
            ORDER BY YEAR(e.date) ASC, MONTH(e.date) ASC
            """)
    List<MonthTotal> sumByMonth(@Param("from") LocalDate from, @Param("to") LocalDate to);
}
