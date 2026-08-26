package com.siddnotani.expensetracker.bff;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.UUID;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class BudgetEndpointsWireMockTest {
    private static final UUID GROCERIES = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID RENT = UUID.fromString("10000000-0000-0000-0000-000000000002");
    private static final UUID TRAVEL = UUID.fromString("10000000-0000-0000-0000-000000000003");

    private static final WireMockServer budgetServer = new WireMockServer(
            com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig().dynamicPort());
    private static final WireMockServer transactionServer = new WireMockServer(
            com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig().dynamicPort());

    static {
        budgetServer.start();
        transactionServer.start();
    }

    @DynamicPropertySource
    static void downstreamProperties(DynamicPropertyRegistry registry) {
        registry.add("downstream.budget-url", budgetServer::baseUrl);
        registry.add("downstream.transaction-url", transactionServer::baseUrl);
    }

    @AfterAll
    static void stopServers() {
        budgetServer.stop();
        transactionServer.stop();
    }

    @LocalServerPort
    int port;
    @Autowired
    TestRestTemplate restTemplate;
    @MockBean
    com.siddnotani.expensetracker.bff.AuthProxyService authProxyService;
    private String cookieHeader;

    @BeforeEach
    void resetServers() {
        budgetServer.resetAll();
        transactionServer.resetAll();
        when(authProxyService.authenticate(any())).thenReturn(
                new com.siddnotani.expensetracker.bff.AuthProxyService.UserResponse(
                        UUID.fromString("90000000-0000-0000-0000-000000000001"),
                        "test@example.com",
                        "Test User"));
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
    }

    @Test
    void progressJoinsBudgetOnlySpendOnlyAndNullCategoryRows() {
        stubBudgetEndpoints();
        stubTransactionSummary("""
                {
                  "totalExpenseMinor":2100,
                  "totalIncomeMinor":500,
                  "netMinor":-1600,
                  "byCategory":[
                    {"categoryId":"10000000-0000-0000-0000-000000000001","totalMinor":1200},
                    {"categoryId":"10000000-0000-0000-0000-000000000003","totalMinor":700},
                    {"categoryId":null,"totalMinor":200}
                  ]
                }
                """);

        ResponseEntity<String> response = authenticatedGet(
                "/api/budgets/progress?period=2025-01");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody())
                .contains("\"totalBudgetedMinor\":1500")
                .contains("\"totalSpentMinor\":2100")
                .contains("\"categoryName\":\"Rent\"")
                .contains("\"spentMinor\":0")
                .contains("\"categoryName\":\"Travel\"")
                .contains("\"budgetedMinor\":0")
                .contains("\"categoryName\":\"Uncategorised\"")
                .contains("\"overBudget\":true");
        budgetServer.verify(com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor(
                        urlPathEqualTo("/internal/budgets"))
                .withQueryParam("period", equalTo("2025-01"))
                .withHeader("Authorization", matching("Bearer .+")));
    }

    @Test
    void dashboardUsesTheSameProgressComposition() {
        stubBudgetEndpoints();
        stubTransactionSummary("""
                {
                  "totalExpenseMinor":2100,
                  "totalIncomeMinor":500,
                  "netMinor":-1600,
                  "byCategory":[
                    {"categoryId":"10000000-0000-0000-0000-000000000001","totalMinor":1200},
                    {"categoryId":"10000000-0000-0000-0000-000000000003","totalMinor":700},
                    {"categoryId":null,"totalMinor":200}
                  ]
                }
                """);

        ResponseEntity<String> response = authenticatedGet("/api/dashboard?period=2025-01");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody())
                .contains("\"totalSpentMinor\":2100")
                .contains("\"totalIncomeMinor\":500")
                .contains("\"netMinor\":-1600")
                .contains("\"topCategories\"")
                .contains("\"budgetProgress\"");
    }

    @Test
    void downstreamFailureReturnsBadGatewayProblemDetail() {
        budgetServer.stubFor(get(urlPathEqualTo("/internal/budgets"))
                .willReturn(aResponse().withStatus(500)));
        budgetServer.stubFor(get(urlPathEqualTo("/internal/categories"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody("[]")));
        stubTransactionSummary("""
                {
                  "totalExpenseMinor":0,
                  "totalIncomeMinor":0,
                  "netMinor":0,
                  "byCategory":[]
                }
                """);

        ResponseEntity<String> response = authenticatedGet(
                "/api/budgets/progress?period=2025-01");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
        assertThat(response.getBody()).contains("\"status\":502");
        assertThat(response.getBody()).doesNotContain("\"categories\"");
    }

    private void stubBudgetEndpoints() {
        budgetServer.stubFor(get(urlPathEqualTo("/internal/budgets"))
                .withQueryParam("period", equalTo("2025-01"))
                .withHeader("Authorization", matching("Bearer .+"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody("""
                                [
                                  {"id":"20000000-0000-0000-0000-000000000001","categoryId":"10000000-0000-0000-0000-000000000001","categoryName":"Groceries","amountMinor":1000},
                                  {"id":"20000000-0000-0000-0000-000000000002","categoryId":"10000000-0000-0000-0000-000000000002","categoryName":"Rent","amountMinor":500}
                                ]
                                """)));
        budgetServer.stubFor(get(urlPathEqualTo("/internal/categories"))
                .withQueryParam("includeArchived", equalTo("true"))
                .withHeader("Authorization", matching("Bearer .+"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody("""
                                [
                                  {"id":"10000000-0000-0000-0000-000000000001","name":"Groceries","color":null,"archived":false},
                                  {"id":"10000000-0000-0000-0000-000000000002","name":"Rent","color":null,"archived":false},
                                  {"id":"10000000-0000-0000-0000-000000000003","name":"Travel","color":null,"archived":true}
                                ]
                                """)));
    }

    private void stubTransactionSummary(String body) {
        transactionServer.stubFor(get(urlPathEqualTo("/internal/transactions/summary"))
                .withQueryParam("from", equalTo("2025-01-01"))
                .withQueryParam("to", equalTo("2025-01-31"))
                .withHeader("Authorization", matching("Bearer .+"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                .withBody(body)));
    }

    private ResponseEntity<String> authenticatedGet(String path) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.COOKIE, cookieHeader);
        return restTemplate.exchange(
                "http://localhost:" + port + path,
                org.springframework.http.HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class);
    }
}
