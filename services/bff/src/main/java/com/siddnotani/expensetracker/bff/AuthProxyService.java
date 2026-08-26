package com.siddnotani.expensetracker.bff;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
class AuthProxyService {
    private final WebClient authWebClient;
    private final ObjectMapper objectMapper;

    AuthProxyService(WebClient authWebClient, ObjectMapper objectMapper) {
        this.authWebClient = authWebClient;
        this.objectMapper = objectMapper;
    }

    UserResponse register(RegisterRequest request) {
        try {
            return authWebClient.post()
                    .uri("/internal/users")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(UserResponse.class)
                    .block();
        } catch (WebClientResponseException exception) {
            throw downstreamException(exception);
        }
    }

    UserResponse authenticate(LoginRequest request) {
        try {
            return authWebClient.post()
                    .uri("/internal/users/authenticate")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(UserResponse.class)
                    .block();
        } catch (WebClientResponseException exception) {
            if (exception.getStatusCode().is4xxClientError()) {
                throw new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Invalid email or password");
            }
            throw exception;
        }
    }

    UserResponse get(UUID id) {
        return authWebClient.get()
                .uri("/internal/users/{id}", id)
                .retrieve()
                .bodyToMono(UserResponse.class)
                .block();
    }

    private ResponseStatusException downstreamException(WebClientResponseException exception) {
        String detail = exception.getResponseBodyAsString();
        try {
            JsonNode body = objectMapper.readTree(detail);
            if (body.hasNonNull("detail")) {
                detail = body.get("detail").asText();
            }
        } catch (Exception ignored) {
            // Preserve the downstream status when it does not return valid ProblemDetail JSON.
        }
        return new ResponseStatusException(exception.getStatusCode(), detail);
    }

    record RegisterRequest(String email, String password, String displayName) {
    }

    record LoginRequest(String email, String password) {
    }

    record UserResponse(UUID id, String email, String displayName) {
    }
}
