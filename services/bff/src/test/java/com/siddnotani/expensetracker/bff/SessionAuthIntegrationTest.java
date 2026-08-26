package com.siddnotani.expensetracker.bff;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SessionAuthIntegrationTest {
    private static final WireMockServer wireMock = new WireMockServer(0);
    private static final UUID USER_ID = UUID.randomUUID();

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate rest;

    @BeforeAll
    static void startWireMock() {
        wireMock.start();
    }

    @AfterAll
    static void stopWireMock() {
        wireMock.stop();
    }

    @DynamicPropertySource
    static void downstreamProperties(DynamicPropertyRegistry registry) {
        registry.add("downstream.auth-url", () -> wireMock.baseUrl());
    }

    @Test
    void loginMeAndLogoutUseSessionCookie() {
        stubAuthenticate();
        HttpHeaders loginHeaders = new HttpHeaders();
        loginHeaders.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
        ResponseEntity<String> login = rest.exchange(
                url("/api/auth/login"),
                HttpMethod.POST,
                new HttpEntity<>(
                        "{\"email\":\"alice@example.com\",\"password\":\"a-long-enough-password\"}",
                        loginHeaders),
                String.class);

        assertEquals(HttpStatus.OK, login.getStatusCode());
        String sessionCookie = cookie(login, "session");
        assertNotNull(sessionCookie);
        String setCookie = String.join("; ", login.getHeaders().get(HttpHeaders.SET_COOKIE));
        assertTrue(setCookie.contains("HttpOnly"));
        assertTrue(setCookie.contains("SameSite=Lax"));
        assertTrue(setCookie.contains("Path=/"));
        assertFalse(login.getBody().contains("a-long-enough-password"));
        assertFalse(login.getBody().contains("$2a$"));

        HttpHeaders sessionHeaders = new HttpHeaders();
        sessionHeaders.add(HttpHeaders.COOKIE, sessionCookie);
        ResponseEntity<String> me = rest.exchange(
                url("/api/auth/me"), HttpMethod.GET, new HttpEntity<>(sessionHeaders), String.class);
        assertEquals(HttpStatus.OK, me.getStatusCode());
        assertTrue(me.getBody().contains("alice@example.com"));

        ResponseEntity<String> noCookie = rest.getForEntity(url("/api/auth/me"), String.class);
        assertEquals(HttpStatus.UNAUTHORIZED, noCookie.getStatusCode());

        HttpHeaders logoutHeaders = new HttpHeaders();
        logoutHeaders.add(HttpHeaders.COOKIE, sessionCookie);
        logoutHeaders.add(HttpHeaders.COOKIE, cookie(login, "XSRF-TOKEN"));
        logoutHeaders.add("X-XSRF-TOKEN", cookieValue(login, "XSRF-TOKEN"));
        ResponseEntity<Void> logout = rest.exchange(
                url("/api/auth/logout"), HttpMethod.POST, new HttpEntity<>(logoutHeaders), Void.class);
        assertEquals(HttpStatus.NO_CONTENT, logout.getStatusCode());

        ResponseEntity<String> oldSession = rest.exchange(
                url("/api/auth/me"), HttpMethod.GET, new HttpEntity<>(sessionHeaders), String.class);
        assertEquals(HttpStatus.UNAUTHORIZED, oldSession.getStatusCode());
    }

    @Test
    void logoutRequiresCsrfAndLoginDoesNot() {
        stubAuthenticate();
        ResponseEntity<String> login = rest.postForEntity(
                url("/api/auth/login"),
                new HttpEntity<>(
                        "{\"email\":\"alice@example.com\",\"password\":\"a-long-enough-password\"}",
                        jsonHeaders()),
                String.class);
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.COOKIE, cookie(login, "session"));
        ResponseEntity<String> forbidden = rest.exchange(
                url("/api/auth/logout"), HttpMethod.POST, new HttpEntity<>(headers), String.class);
        assertEquals(HttpStatus.FORBIDDEN, forbidden.getStatusCode());
    }

    private void stubAuthenticate() {
        wireMock.resetAll();
        wireMock.stubFor(post(urlEqualTo("/internal/users/authenticate"))
                .withRequestBody(equalToJson(
                        "{\"email\":\"alice@example.com\",\"password\":\"a-long-enough-password\"}"))
                .willReturn(com.github.tomakehurst.wiremock.client.WireMock.aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"id":"%s","email":"alice@example.com","displayName":"Alice"}
                                """.formatted(USER_ID))));
    }

    private String url(String path) {
        return "http://localhost:" + port + path;
    }

    private static HttpHeaders jsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
        return headers;
    }

    private static String cookie(ResponseEntity<?> response, String name) {
        String value = cookieValue(response, name);
        return name + "=" + value;
    }

    private static String cookieValue(ResponseEntity<?> response, String name) {
        for (String header : response.getHeaders().get(HttpHeaders.SET_COOKIE)) {
            String prefix = name + "=";
            if (header.startsWith(prefix)) {
                return header.substring(prefix.length()).split(";", 2)[0];
            }
        }
        throw new AssertionError("Missing cookie " + name);
    }
}
