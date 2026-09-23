package com.tracker;

import static org.assertj.core.api.Assertions.assertThat;

import com.tracker.expense.model.CategoryType;
import com.tracker.expense.repository.CategoryRepository;
import com.tracker.expense.repository.ExpenseRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class SeedDataTest {

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ExpenseRepository expenseRepository;

    @Test
    void seedsAllCategories() {
        assertThat(categoryRepository.count()).isEqualTo(CategoryType.values().length);
        for (CategoryType type : CategoryType.values()) {
            assertThat(categoryRepository.findByName(type.name())).isPresent();
        }
    }

    @Test
    void seedsSampleExpenses() {
        assertThat(expenseRepository.count()).isGreaterThan(0);
    }
}
