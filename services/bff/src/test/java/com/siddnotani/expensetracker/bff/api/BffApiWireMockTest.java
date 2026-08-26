package com.siddnotani.expensetracker.bff.api;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.net.http.HttpClient;
import java.util.List;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.deleteRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class BffApiWireMockTest {
    private static final UUID USER_ID =
            UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID ACCOUNT_ID =
            UUID.fromString("20000000-0000-0000-0000-000000000001");
    private static final UUID OTHER_ACCOUNT_ID =
            UUID.fromString("20000000-0000-0000-0000-000000000002");
    private static final UUID TRANSACTION_ID =
            UUID.fromString("30000000-0000-0000-0000-000000000001");
    private static final WireMockServer accounts = new WireMockServer(
            WireMockConfiguration.options().dynamicPort());
    private static final WireMockServer transactions = new WireMockServer(
            WireMockConfiguration.options().dynamicPort());

    @Autowired
    private TestRestTemplate restTemplate;

    @MockBean
    private com.siddnotani.expensetracker.bff.AuthProxyService authProxyService;

    private String cookieHeader;
    private String csrfToken;

    @LocalServerPort
    private int port;

    @BeforeAll
    static void startServers() {
        accounts.start();
        transactions.start();
    }

    @AfterAll
    static void stopServers() {
        accounts.stop();
        transactions.stop();
    }

    @DynamicPropertySource
    static void downstreamProperties(DynamicPropertyRegistry registry) {
        if (!accounts.isRunning()) {
            accounts.start();
        }
        if (!transactions.isRunning()) {
            transactions.start();
        }
        registry.add("downstream.accounts-url", accounts::baseUrl);
        registry.add("downstream.transaction-url", transactions::baseUrl);
    }

    @BeforeEach
    void reset() {
        accounts.resetAll();
        transactions.resetAll();
        restTemplate.getRestTemplate().setRequestFactory(
                new JdkClientHttpRequestFactory(HttpClient.newHttpClient()));
        when(authProxyService.authenticate(any())).thenReturn(
                new com.siddnotani.expensetracker.bff.AuthProxyService.UserResponse(
                        USER_ID, "test@example.com", "Test User"));
        ResponseEntity<Void> login = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/auth/login",
                new com.siddnotani.expensetracker.bff.AuthProxyService.LoginRequest(
                        "test@example.com", "password"),
                Void.class);
        List<String> cookies = login.getHeaders().get(HttpHeaders.SET_COOKIE);
        cookieHeader = cookies.stream()
                .map(cookie -> cookie.substring(0, cookie.indexOf(';')))
                .reduce((left, right) -> left + "; " + right)
                .orElseThrow();
        csrfToken = cookies.stream()
                .map(cookie -> cookie.substring(0, cookie.indexOf(';')))
                .filter(cookie -> cookie.startsWith("XSRF-TOKEN="))
                .map(cookie -> cookie.substring("XSRF-TOKEN=".length()))
                .findFirst()
                .orElseThrow();
    }

    @Test
    void createExpenseCoordinatesBalance() {
        stubTransactionCreate(transaction(TRANSACTION_ID, ACCOUNT_ID, "EXPENSE", 2500));
        stubBalanceAdjustment(ACCOUNT_ID, -2500);

        ResponseEntity<TransactionResponse> response = exchange(
                HttpMethod.POST,
                "/api/transactions",
                """
                {"accountId":"20000000-0000-0000-0000-000000000001",
                 "direction":"EXPENSE","amountMinor":2500,"currency":"USD",
                 "occurredOn":"2025-01-10","description":"Lunch"}
                """,
                new ParameterizedTypeReference<>() {
                });

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(TRANSACTION_ID, response.getBody().id());
        transactions.verify(postRequestedFor(urlEqualTo("/internal/transactions"))
                .withHeader("Authorization", matching("Bearer .+")));
        accounts.verify(postRequestedFor(urlEqualTo(
                        "/internal/accounts/" + ACCOUNT_ID + "/balance-adjustments"))
                .withRequestBody(matchingJsonPath("$.deltaMinor", equalTo("-2500"))));
    }

    @Test
    void updateSameAccountAdjustsDifference() {
        stubTransactionGet(transaction(TRANSACTION_ID, ACCOUNT_ID, "EXPENSE", 2500));
        stubTransactionUpdate(transaction(TRANSACTION_ID, ACCOUNT_ID, "EXPENSE", 3000));
        stubBalanceAdjustment(ACCOUNT_ID, -500);

        ResponseEntity<TransactionResponse> response = exchange(
                HttpMethod.PUT,
                "/api/transactions/" + TRANSACTION_ID,
                transactionRequest(ACCOUNT_ID, "EXPENSE", 3000),
                new ParameterizedTypeReference<>() {
                });

        assertEquals(HttpStatus.OK, response.getStatusCode());
        accounts.verify(1, postRequestedFor(urlEqualTo(
                        "/internal/accounts/" + ACCOUNT_ID + "/balance-adjustments"))
                .withRequestBody(matchingJsonPath("$.deltaMinor", equalTo("-500"))));
    }

    @Test
    void updateChangingAccountAdjustsBothAccounts() {
        stubTransactionGet(transaction(TRANSACTION_ID, ACCOUNT_ID, "EXPENSE", 2500));
        stubTransactionUpdate(transaction(TRANSACTION_ID, OTHER_ACCOUNT_ID, "INCOME", 4000));
        stubBalanceAdjustment(ACCOUNT_ID, 2500);
        stubBalanceAdjustment(OTHER_ACCOUNT_ID, 4000);

        ResponseEntity<TransactionResponse> response = exchange(
                HttpMethod.PUT,
                "/api/transactions/" + TRANSACTION_ID,
                transactionRequest(OTHER_ACCOUNT_ID, "INCOME", 4000),
                new ParameterizedTypeReference<>() {
                });

        assertEquals(HttpStatus.OK, response.getStatusCode());
        accounts.verify(postRequestedFor(urlEqualTo(
                        "/internal/accounts/" + ACCOUNT_ID + "/balance-adjustments"))
                .withRequestBody(matchingJsonPath("$.deltaMinor", equalTo("2500"))));
        accounts.verify(postRequestedFor(urlEqualTo(
                        "/internal/accounts/" + OTHER_ACCOUNT_ID + "/balance-adjustments"))
                .withRequestBody(matchingJsonPath("$.deltaMinor", equalTo("4000"))));
    }

    @Test
    void deleteAppliesInverseAdjustment() {
        stubTransactionGet(transaction(TRANSACTION_ID, ACCOUNT_ID, "INCOME", 1250));
        transactions.stubFor(delete(urlEqualTo("/internal/transactions/" + TRANSACTION_ID))
                .willReturn(aResponse().withStatus(HttpStatus.NO_CONTENT.value())));
        stubBalanceAdjustment(ACCOUNT_ID, -1250);

        ResponseEntity<Void> response = exchange(
                HttpMethod.DELETE,
                "/api/transactions/" + TRANSACTION_ID,
                null,
                new ParameterizedTypeReference<>() {
                });

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        accounts.verify(postRequestedFor(urlEqualTo(
                        "/internal/accounts/" + ACCOUNT_ID + "/balance-adjustments"))
                .withRequestBody(matchingJsonPath("$.deltaMinor", equalTo("-1250"))));
    }

    @Test
    void failedAdjustmentReturnsStaleBalanceProblem() {
        stubTransactionCreate(transaction(TRANSACTION_ID, ACCOUNT_ID, "EXPENSE", 2500));
        accounts.stubFor(post(urlEqualTo(
                        "/internal/accounts/" + ACCOUNT_ID + "/balance-adjustments"))
                .willReturn(aResponse().withStatus(HttpStatus.INTERNAL_SERVER_ERROR.value())
                        .withHeader("Content-Type", MediaType.APPLICATION_PROBLEM_JSON_VALUE)
                        .withBody("""
                        {"type":"about:blank","title":"Internal Server Error",
                         "status":500,"detail":"account unavailable"}
                        """)));

        ResponseEntity<String> response = exchange(
                HttpMethod.POST,
                "/api/transactions",
                transactionRequest(ACCOUNT_ID, "EXPENSE", 2500),
                new ParameterizedTypeReference<>() {
                });

        assertEquals(HttpStatus.BAD_GATEWAY, response.getStatusCode());
        assertTrue(response.getBody().contains("account balance is stale"));
        transactions.verify(1, postRequestedFor(urlEqualTo("/internal/transactions")));
    }

    @Test
    void listAndSummaryPassThroughFiltersAndMinorFields() {
        transactions.stubFor(get(urlPathEqualTo("/internal/transactions"))
                .withQueryParam("from", equalTo("2025-01-01"))
                .withQueryParam("to", equalTo("2025-01-31"))
                .withQueryParam("accountId", equalTo(ACCOUNT_ID.toString()))
                .withQueryParam("categoryId", equalTo("40000000-0000-0000-0000-000000000001"))
                .withQueryParam("direction", equalTo("EXPENSE"))
                .withQueryParam("page", equalTo("2"))
                .withQueryParam("size", equalTo("10"))
                .willReturn(json("""
                {"items":[{"id":"30000000-0000-0000-0000-000000000001",
                  "accountId":"20000000-0000-0000-0000-000000000001",
                  "categoryId":"40000000-0000-0000-0000-000000000001",
                  "direction":"EXPENSE","amountMinor":12345,"currency":"USD",
                  "occurredOn":"2025-01-10","description":"Lunch",
                  "createdAt":"2025-01-10T12:00:00Z"}],
                  "total":1,"page":2,"size":10}
                """)));
        transactions.stubFor(get(urlPathEqualTo("/internal/transactions/summary"))
                .withQueryParam("from", equalTo("2025-01-01"))
                .withQueryParam("to", equalTo("2025-01-31"))
                .willReturn(json("""
                {"totalExpenseMinor":12345,"totalIncomeMinor":5000,"netMinor":-7345,
                 "byCategory":[{"categoryId":null,"totalMinor":12345}]}
                """)));

        ResponseEntity<TransactionPageResponse> listResponse = exchange(
                HttpMethod.GET,
                "/api/transactions?from=2025-01-01&to=2025-01-31&accountId=" + ACCOUNT_ID
                        + "&categoryId=40000000-0000-0000-0000-000000000001"
                        + "&direction=EXPENSE&page=2&size=10",
                null,
                new ParameterizedTypeReference<>() {
                });
        ResponseEntity<SummaryResponse> summaryResponse = exchange(
                HttpMethod.GET,
                "/api/transactions/summary?from=2025-01-01&to=2025-01-31",
                null,
                new ParameterizedTypeReference<>() {
                });

        assertEquals(12345L, listResponse.getBody().items().get(0).amountMinor());
        assertEquals(-7345L, summaryResponse.getBody().netMinor());
        transactions.verify(getRequestedFor(urlPathEqualTo("/internal/transactions")));
        transactions.verify(getRequestedFor(urlPathEqualTo("/internal/transactions/summary")));
    }

    @Test
    void accountListPassesMinorBalanceThrough() {
        accounts.stubFor(get(urlEqualTo("/internal/accounts?includeArchived=true"))
                .willReturn(json("""
                [{"id":"20000000-0000-0000-0000-000000000001","name":"Checking",
                  "type":"CHECKING","currency":"USD","balanceMinor":98765,
                  "archived":false,"createdAt":"2025-01-01T00:00:00Z"}]
                """)));

        ResponseEntity<AccountResponse[]> response = exchange(
                HttpMethod.GET,
                "/api/accounts?includeArchived=true",
                null,
                new ParameterizedTypeReference<>() {
                });

        assertEquals(98765L, response.getBody()[0].balanceMinor());
        accounts.verify(getRequestedFor(urlEqualTo("/internal/accounts?includeArchived=true")));
    }

    private void stubTransactionCreate(String response) {
        transactions.stubFor(post(urlEqualTo("/internal/transactions")).willReturn(json(response)));
    }

    private void stubTransactionGet(String response) {
        transactions.stubFor(get(urlEqualTo("/internal/transactions/" + TRANSACTION_ID))
                .willReturn(json(response)));
    }

    private void stubTransactionUpdate(String response) {
        transactions.stubFor(put(urlEqualTo("/internal/transactions/" + TRANSACTION_ID))
                .willReturn(json(response)));
    }

    private void stubBalanceAdjustment(UUID accountId, long deltaMinor) {
        accounts.stubFor(post(urlEqualTo(
                        "/internal/accounts/" + accountId + "/balance-adjustments"))
                .withRequestBody(matchingJsonPath("$.deltaMinor", equalTo(Long.toString(deltaMinor))))
                .willReturn(json("""
                {"accountId":"%s","balanceMinor":10000}
                """.formatted(accountId))));
    }

    private String transaction(UUID id, UUID accountId, String direction, long amountMinor) {
        return """
                {"id":"%s","accountId":"%s","categoryId":null,"direction":"%s",
                 "amountMinor":%d,"currency":"USD","occurredOn":"2025-01-10",
                 "description":"Test","createdAt":"2025-01-10T12:00:00Z"}
                """.formatted(id, accountId, direction, amountMinor);
    }

    private String transactionRequest(UUID accountId, String direction, long amountMinor) {
        return """
                {"accountId":"%s","direction":"%s","amountMinor":%d,"currency":"USD",
                 "occurredOn":"2025-01-10","description":"Test"}
                """.formatted(accountId, direction, amountMinor);
    }

    private static com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder json(
            String body) {
        return aResponse()
                .withStatus(HttpStatus.OK.value())
                .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .withBody(body);
    }

    private <T> ResponseEntity<T> exchange(
            HttpMethod method,
            String path,
            String body,
            ParameterizedTypeReference<T> responseType) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(HttpHeaders.COOKIE, cookieHeader);
        headers.set("X-XSRF-TOKEN", csrfToken);
        return restTemplate.exchange(
                "http://localhost:" + port + path,
                method,
                new HttpEntity<>(body, headers),
                responseType);
    }
}
