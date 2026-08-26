package com.siddnotani.expensetracker.budget;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.siddnotani.expensetracker.shared.auth.InternalJwt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BudgetPostgresIntegrationTest {
    private static final UUID USER_ONE = UUID.fromString("00000000-0000-0000-0000-000000000011");
    private static final UUID USER_TWO = UUID.fromString("00000000-0000-0000-0000-000000000022");

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    static {
        postgres.start();
    }

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.flyway.enabled", () -> true);
    }

    @Autowired
    MockMvc mockMvc;
    @Autowired
    InternalJwt internalJwt;
    @Autowired
    JdbcTemplate jdbcTemplate;
    @Autowired
    ObjectMapper objectMapper;

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.execute("TRUNCATE budget.monthly_budgets, budget.categories");
    }

    @Test
    void upsertNormalisesPeriodAndUpdatesExistingRow() throws Exception {
        UUID category = createCategory(USER_ONE, "Groceries");

        putBudget(USER_ONE, category, "2025-01", 1000);
        putBudget(USER_ONE, category, "2025-01", 2500);

        mockMvc.perform(get("/internal/budgets")
                        .param("period", "2025-01")
                        .header(AUTHORIZATION, bearer(USER_ONE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].amountMinor", is(2500)));
        Integer count = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM budget.monthly_budgets WHERE user_id = ? AND category_id = ? AND period = '2025-01-01'",
                Integer.class, USER_ONE, category);
        org.assertj.core.api.Assertions.assertThat(count).isEqualTo(1);
    }

    @Test
    void duplicateCategoryNameReturnsConflictCaseInsensitively() throws Exception {
        createCategory(USER_ONE, "Groceries");

        mockMvc.perform(post("/internal/categories")
                        .header(AUTHORIZATION, bearer(USER_ONE))
                        .contentType("application/json")
                        .content("""
                                {"name":"groceries","color":"#123456"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"));
    }

    @Test
    void badPeriodsReturnProblemDetails() throws Exception {
        mockMvc.perform(get("/internal/budgets")
                        .param("period", "2025-13")
                        .header(AUTHORIZATION, bearer(USER_ONE)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"));
    }

    @Test
    void deletingCategoryCascadesToMonthlyBudgets() throws Exception {
        UUID category = createCategory(USER_ONE, "Rent");
        putBudget(USER_ONE, category, "2025-01", 50000);

        jdbcTemplate.update("DELETE FROM budget.categories WHERE id = ?", category);

        Integer count = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM budget.monthly_budgets WHERE category_id = ?",
                Integer.class, category);
        org.assertj.core.api.Assertions.assertThat(count).isZero();
    }

    @Test
    void defaultsAndCopyAreIdempotent() throws Exception {
        mockMvc.perform(post("/internal/categories/defaults")
                        .header(AUTHORIZATION, bearer(USER_ONE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(7)));
        mockMvc.perform(post("/internal/categories/defaults")
                        .header(AUTHORIZATION, bearer(USER_ONE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(7)));

        UUID groceries = categoryIdByName(USER_ONE, "Groceries");
        putBudget(USER_ONE, groceries, "2025-01", 10000);
        copy(USER_ONE, "2025-01", "2025-02");
        copy(USER_ONE, "2025-01", "2025-02");

        Integer count = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM budget.monthly_budgets WHERE user_id = ? AND period = '2025-02-01'",
                Integer.class, USER_ONE);
        org.assertj.core.api.Assertions.assertThat(count).isEqualTo(1);
    }

    @Test
    void anotherUserCannotSeeOrChangeRows() throws Exception {
        UUID category = createCategory(USER_ONE, "Private");
        putBudget(USER_ONE, category, "2025-01", 100);

        mockMvc.perform(patch("/internal/categories/{id}", category)
                        .header(AUTHORIZATION, bearer(USER_TWO))
                        .contentType("application/json")
                        .content("""
                                {"name":"Stolen"}
                                """))
                .andExpect(status().isNotFound());
        mockMvc.perform(put("/internal/budgets")
                        .header(AUTHORIZATION, bearer(USER_TWO))
                        .contentType("application/json")
                        .content("""
                                {"categoryId":"%s","period":"2025-01","amountMinor":200}
                                """.formatted(category)))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/internal/budgets/{id}", budgetId(category))
                        .header(AUTHORIZATION, bearer(USER_TWO)))
                .andExpect(status().isNotFound());
    }

    private UUID createCategory(UUID userId, String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/internal/categories")
                        .header(AUTHORIZATION, bearer(userId))
                        .contentType("application/json")
                        .content("""
                                {"name":"%s","color":"#123456"}
                                """.formatted(name)))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return UUID.fromString(body.get("id").asText());
    }

    private void putBudget(UUID userId, UUID categoryId, String period, long amount) throws Exception {
        mockMvc.perform(put("/internal/budgets")
                        .header(AUTHORIZATION, bearer(userId))
                        .contentType("application/json")
                        .content("""
                                {"categoryId":"%s","period":"%s","amountMinor":%d}
                                """.formatted(categoryId, period, amount)))
                .andExpect(status().isOk());
    }

    private void copy(UUID userId, String from, String to) throws Exception {
        mockMvc.perform(post("/internal/budgets/copy")
                        .header(AUTHORIZATION, bearer(userId))
                        .contentType("application/json")
                        .content("""
                                {"fromPeriod":"%s","toPeriod":"%s"}
                                """.formatted(from, to)))
                .andExpect(status().isOk());
    }

    private UUID categoryIdByName(UUID userId, String name) {
        return jdbcTemplate.queryForObject(
                "SELECT id FROM budget.categories WHERE user_id = ? AND name = ?",
                (resultSet, rowNum) -> (UUID) resultSet.getObject("id"), userId, name);
    }

    private UUID budgetId(UUID categoryId) {
        return jdbcTemplate.queryForObject(
                "SELECT id FROM budget.monthly_budgets WHERE category_id = ?",
                (resultSet, rowNum) -> (UUID) resultSet.getObject("id"), categoryId);
    }

    private String bearer(UUID userId) {
        return "Bearer " + internalJwt.sign(userId);
    }
}
