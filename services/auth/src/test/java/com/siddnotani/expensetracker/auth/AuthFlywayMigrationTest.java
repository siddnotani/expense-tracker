package com.siddnotani.expensetracker.auth;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Testcontainers
class AuthFlywayMigrationTest {
    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Test
    void usersEmailIndexIsCaseInsensitive() {
        String hash = "$2a$12$not-a-real-password-hash";
        jdbcTemplate.update(
                "insert into auth.users (id, email, password_hash, display_name) values (?, ?, ?, ?)",
                UUID.randomUUID(), "Case@example.com", hash, "First");
        assertThrows(
                RuntimeException.class,
                () -> jdbcTemplate.update(
                        "insert into auth.users (id, email, password_hash, display_name) values (?, ?, ?, ?)",
                        UUID.randomUUID(), "case@EXAMPLE.com", hash, "Second"));
    }
}
