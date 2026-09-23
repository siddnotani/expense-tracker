package com.tracker.analytics.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tracker.expense.model.Category;
import com.tracker.expense.model.Expense;
import com.tracker.expense.repository.CategoryRepository;
import com.tracker.expense.repository.ExpenseRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class AnalyticsControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ExpenseRepository expenseRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    private Category food;
    private Category transport;
    private Category housing;

    @BeforeEach
    void cleanAndSeedCategories() {
        expenseRepository.deleteAll();
        categoryRepository.deleteAll();
        food = categoryRepository.save(new Category("FOOD", "Food"));
        transport = categoryRepository.save(new Category("TRANSPORT", "Transport"));
        housing = categoryRepository.save(new Category("HOUSING", "Housing"));
    }

    @Test
    void returnsEmptySummaryWhenNoExpenses() throws Exception {
        mockMvc.perform(get("/api/analytics/summary"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().json("""
                        {
                          "totalSpend": 0.00,
                          "stats": { "count": 0, "average": 0.00, "min": 0.00, "max": 0.00 },
                          "byCategory": [],
                          "byMonth": []
                        }
                        """, true));
    }

    @Test
    void returnsFullSummaryWithOrdering() throws Exception {
        expense("12.50", food, LocalDate.of(2026, 3, 5));
        expense("7.50", food, LocalDate.of(2026, 4, 10));
        expense("30.00", transport, LocalDate.of(2026, 4, 12));
        expense("450.00", housing, LocalDate.of(2026, 2, 1));

        mockMvc.perform(get("/api/analytics/summary"))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {
                          "totalSpend": 500.00,
                          "stats": { "count": 4, "average": 125.00, "min": 7.50, "max": 450.00 },
                          "byCategory": [
                            { "category": "HOUSING", "displayName": "Housing", "total": 450.00, "percentage": 90.0 },
                            { "category": "TRANSPORT", "displayName": "Transport", "total": 30.00, "percentage": 6.0 },
                            { "category": "FOOD", "displayName": "Food", "total": 20.00, "percentage": 4.0 }
                          ],
                          "byMonth": [
                            { "month": "2026-02", "total": 450.00 },
                            { "month": "2026-03", "total": 12.50 },
                            { "month": "2026-04", "total": 37.50 }
                          ]
                        }
                        """, true))
                .andExpect(jsonPath("$.byCategory[0].category", is("HOUSING")))
                .andExpect(jsonPath("$.byMonth", hasSize(3)))
                .andExpect(jsonPath("$.byMonth[0].month", is("2026-02")))
                .andExpect(jsonPath("$.byMonth[2].month", is("2026-04")));
    }

    @Test
    void filtersByInclusiveDateRange() throws Exception {
        expense("12.50", food, LocalDate.of(2026, 3, 5));
        expense("7.50", food, LocalDate.of(2026, 4, 10));
        expense("30.00", transport, LocalDate.of(2026, 4, 12));
        expense("450.00", housing, LocalDate.of(2026, 2, 1));

        mockMvc.perform(get("/api/analytics/summary").param("from", "2026-03-05").param("to", "2026-04-12"))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {
                          "totalSpend": 50.00,
                          "stats": { "count": 3, "average": 16.67, "min": 7.50, "max": 30.00 },
                          "byCategory": [
                            { "category": "TRANSPORT", "displayName": "Transport", "total": 30.00, "percentage": 60.0 },
                            { "category": "FOOD", "displayName": "Food", "total": 20.00, "percentage": 40.0 }
                          ],
                          "byMonth": [
                            { "month": "2026-03", "total": 12.50 },
                            { "month": "2026-04", "total": 37.50 }
                          ]
                        }
                        """, true));
    }

    @Test
    void rejectsFromAfterTo() throws Exception {
        mockMvc.perform(get("/api/analytics/summary").param("from", "2026-05-01").param("to", "2026-04-01"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("Bad Request")))
                .andExpect(jsonPath("$.message", is("'from' must be on or before 'to'")));
    }

    @Test
    void rejectsMalformedDate() throws Exception {
        mockMvc.perform(get("/api/analytics/summary").param("from", "not-a-date"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)));
    }

    @Test
    void allowsCorsFromFrontendOrigin() throws Exception {
        mockMvc.perform(options("/api/analytics/summary")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
    }

    private void expense(String amount, Category category, LocalDate date) {
        expenseRepository.save(new Expense(new BigDecimal(amount), category, "fixture", date));
    }
}
