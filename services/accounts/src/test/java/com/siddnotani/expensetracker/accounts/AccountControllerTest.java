package com.siddnotani.expensetracker.accounts;

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

import java.util.List;
import java.net.http.HttpClient;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class AccountControllerTest {
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
    AccountRepository accountRepository;

    @org.junit.jupiter.api.BeforeEach
    void enablePatchRequests() {
        restTemplate.getRestTemplate().setRequestFactory(
                new JdkClientHttpRequestFactory(HttpClient.newHttpClient()));
    }

    @AfterEach
    void clean() {
        accountRepository.deleteAll();
    }

    @Test
    void supportsCreateListGetPatchAndArchive() {
        UUID user = UUID.randomUUID();
        ResponseEntity<AccountResponse> created = exchange(
                user, HttpMethod.POST, "/internal/accounts",
                new CreateAccountRequest("Checking", AccountType.CHECKING, "usd", 125L),
                AccountResponse.class);
        assertEquals(HttpStatus.CREATED, created.getStatusCode());
        assertNotNull(created.getBody());
        UUID id = created.getBody().id();

        ResponseEntity<List<AccountResponse>> listed = exchange(
                user, HttpMethod.GET, "/internal/accounts", null,
                new ParameterizedTypeReference<>() {});
        assertEquals(1, listed.getBody().size());

        ResponseEntity<AccountResponse> fetched = exchange(
                user, HttpMethod.GET, "/internal/accounts/" + id, null, AccountResponse.class);
        assertEquals("USD", fetched.getBody().currency());

        ResponseEntity<AccountResponse> patched = exchange(
                user, HttpMethod.PATCH, "/internal/accounts/" + id,
                new UpdateAccountRequest("Renamed", null), AccountResponse.class);
        assertEquals("Renamed", patched.getBody().name());

        ResponseEntity<Void> deleted = exchange(
                user, HttpMethod.DELETE, "/internal/accounts/" + id, null, Void.class);
        assertEquals(HttpStatus.NO_CONTENT, deleted.getStatusCode());
        assertTrue(exchange(user, HttpMethod.GET, "/internal/accounts", null,
                new ParameterizedTypeReference<List<AccountResponse>>() {}).getBody().isEmpty());
        assertEquals(1, exchange(user, HttpMethod.GET, "/internal/accounts?includeArchived=true", null,
                new ParameterizedTypeReference<List<AccountResponse>>() {}).getBody().size());
    }

    @Test
    void isolatesAccountsBetweenUsersAndAdjustsBalance() {
        UUID owner = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        AccountResponse account = exchange(owner, HttpMethod.POST, "/internal/accounts",
                new CreateAccountRequest("Cash", AccountType.CASH, null, null),
                AccountResponse.class).getBody();
        assertNotNull(account);

        ResponseEntity<String> get = exchange(
                other, HttpMethod.GET, "/internal/accounts/" + account.id(), null, String.class);
        assertEquals(HttpStatus.NOT_FOUND, get.getStatusCode());
        assertEquals(HttpStatus.NOT_FOUND, exchange(other, HttpMethod.PATCH,
                "/internal/accounts/" + account.id(), new UpdateAccountRequest("Nope", null),
                String.class).getStatusCode());
        assertEquals(HttpStatus.NOT_FOUND, exchange(other, HttpMethod.DELETE,
                "/internal/accounts/" + account.id(), null, String.class).getStatusCode());
        assertEquals(HttpStatus.NOT_FOUND, exchange(other, HttpMethod.POST,
                "/internal/accounts/" + account.id() + "/balance-adjustments",
                new BalanceAdjustmentRequest(50L), String.class).getStatusCode());

        ResponseEntity<BalanceResponse> adjusted = exchange(owner, HttpMethod.POST,
                "/internal/accounts/" + account.id() + "/balance-adjustments",
                new BalanceAdjustmentRequest(50L), BalanceResponse.class);
        assertEquals(50, adjusted.getBody().balanceMinor());
    }

    @Test
    void rejectsDuplicateNamesAndInvalidTypes() {
        UUID user = UUID.randomUUID();
        assertEquals(HttpStatus.CREATED, exchange(user, HttpMethod.POST, "/internal/accounts",
                new CreateAccountRequest("Cash", AccountType.CASH, null, null), AccountResponse.class)
                .getStatusCode());
        assertEquals(HttpStatus.CONFLICT, exchange(user, HttpMethod.POST, "/internal/accounts",
                new CreateAccountRequest("cash", AccountType.CASH, null, null), String.class)
                .getStatusCode());
        HttpHeaders headers = headers(user);
        ResponseEntity<String> invalid = restTemplate.exchange(url("/internal/accounts"), HttpMethod.POST,
                new HttpEntity<>("{\"name\":\"Bad\",\"type\":\"INVALID\"}", headers), String.class);
        assertEquals(HttpStatus.BAD_REQUEST, invalid.getStatusCode());
        assertFalse(invalid.getBody() == null || invalid.getBody().isBlank());
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
