package com.siddnotani.expensetracker.transaction;

import com.siddnotani.expensetracker.shared.auth.InternalJwt;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.net.http.HttpClient;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class TransactionControllerTest {
    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @LocalServerPort
    int port;

    @Autowired
    TestRestTemplate restTemplate;

    @Autowired
    InternalJwt internalJwt;

    @Autowired
    TransactionRepository transactionRepository;

    @org.junit.jupiter.api.BeforeEach
    void enablePatchRequests() {
        restTemplate.getRestTemplate().setRequestFactory(
                new JdkClientHttpRequestFactory(HttpClient.newHttpClient()));
    }

    @AfterEach
    void clean() {
        transactionRepository.deleteAll();
    }

    @Test
    void supportsCrudAndSummaryRoute() {
        UUID user = UUID.randomUUID();
        TransactionRequest request = request(Direction.EXPENSE, 125L, "Lunch");
        ResponseEntity<TransactionResponse> created = exchange(
                user, HttpMethod.POST, "/internal/transactions", request, TransactionResponse.class);
        assertEquals(HttpStatus.CREATED, created.getStatusCode());
        assertNotNull(created.getBody());
        UUID id = created.getBody().id();

        ResponseEntity<TransactionPageResponse> list = exchange(user, HttpMethod.GET,
                "/internal/transactions", null, TransactionPageResponse.class);
        assertEquals(1, list.getBody().total());
        assertEquals(id, list.getBody().items().getFirst().id());

        ResponseEntity<SummaryResponse> summary = exchange(user, HttpMethod.GET,
                "/internal/transactions/summary", null, SummaryResponse.class);
        assertEquals(125, summary.getBody().totalExpenseMinor());

        TransactionResponse updated = exchange(user, HttpMethod.PUT,
                "/internal/transactions/" + id, request(Direction.INCOME, 200L, "Refund"),
                TransactionResponse.class).getBody();
        assertEquals(Direction.INCOME, updated.direction());
        assertEquals(HttpStatus.NO_CONTENT, exchange(user, HttpMethod.DELETE,
                "/internal/transactions/" + id, null, Void.class).getStatusCode());
    }

    @Test
    void rejectsInvalidInput() {
        UUID user = UUID.randomUUID();
        assertEquals(HttpStatus.BAD_REQUEST, exchange(user, HttpMethod.POST,
                "/internal/transactions", request(Direction.EXPENSE, 0L, "Invalid"),
                String.class).getStatusCode());
        HttpHeaders headers = headers(user);
        ResponseEntity<String> invalidDirection = restTemplate.exchange(
                url("/internal/transactions"), HttpMethod.POST,
                new HttpEntity<>("""
                        {"accountId":"%s","direction":"INVALID","amountMinor":1,
                         "occurredOn":"2024-01-01","description":"Invalid"}
                        """.formatted(UUID.randomUUID()), headers),
                String.class);
        assertEquals(HttpStatus.BAD_REQUEST, invalidDirection.getStatusCode());
    }

    @Test
    void isolatesTransactionsBetweenUsersAndListsOnlyOwnedRows() {
        UUID owner = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        TransactionResponse transaction = exchange(owner, HttpMethod.POST,
                "/internal/transactions", request(Direction.EXPENSE, 50L, "Expense"),
                TransactionResponse.class).getBody();
        assertNotNull(transaction);
        String path = "/internal/transactions/" + transaction.id();
        assertEquals(HttpStatus.NOT_FOUND, exchange(other, HttpMethod.GET, path, null,
                String.class).getStatusCode());
        assertEquals(HttpStatus.NOT_FOUND, exchange(other, HttpMethod.PUT, path,
                request(Direction.INCOME, 75L, "Nope"), String.class).getStatusCode());
        assertEquals(HttpStatus.NOT_FOUND, exchange(other, HttpMethod.DELETE, path, null,
                String.class).getStatusCode());
        TransactionPageResponse otherList = exchange(other, HttpMethod.GET,
                "/internal/transactions", null, TransactionPageResponse.class).getBody();
        assertTrue(otherList.items().isEmpty());
    }

    private TransactionRequest request(Direction direction, long amount, String description) {
        return new TransactionRequest(UUID.randomUUID(), direction, amount, null,
                LocalDate.of(2024, 1, 1), description, null);
    }

    private <T> ResponseEntity<T> exchange(
            UUID user, HttpMethod method, String path, Object body, Class<T> type) {
        return restTemplate.exchange(url(path), method, new HttpEntity<>(body, headers(user)), type);
    }

    private <T> ResponseEntity<T> exchange(
            UUID user, HttpMethod method, String path, Object body,
            ParameterizedTypeReference<T> type) {
        return restTemplate.exchange(url(path), method, new HttpEntity<>(body, headers(user)), type);
    }

    private HttpHeaders headers(UUID user) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(internalJwt.sign(user));
        headers.set(HttpHeaders.CONTENT_TYPE, "application/json");
        return headers;
    }

    private String url(String path) {
        return "http://localhost:" + port + path;
    }
}
