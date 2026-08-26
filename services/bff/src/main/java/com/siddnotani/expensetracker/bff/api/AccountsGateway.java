package com.siddnotani.expensetracker.bff.api;

import com.siddnotani.expensetracker.shared.auth.InternalJwt;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Arrays;
import java.util.UUID;

@Component
public class AccountsGateway {
    private final WebClient webClient;
    private final InternalJwt internalJwt;

    AccountsGateway(
            @Qualifier("accountsWebClient") WebClient webClient,
            InternalJwt internalJwt) {
        this.webClient = webClient;
        this.internalJwt = internalJwt;
    }

    public List<AccountResponse> list(UUID userId, boolean includeArchived) {
        return Arrays.stream(execute(webClient.get()
                .uri(uri -> uri.path("/internal/accounts")
                        .queryParam("includeArchived", includeArchived)
                        .build())
                .headers(headers -> authorize(headers, userId)), AccountResponse[].class))
                .toList();
    }

    public AccountResponse create(UUID userId, CreateAccountRequest request) {
        return execute(webClient.post()
                .uri("/internal/accounts")
                .headers(headers -> authorize(headers, userId))
                .bodyValue(request), AccountResponse.class);
    }

    public AccountResponse get(UUID userId, UUID accountId) {
        return execute(webClient.get()
                .uri("/internal/accounts/{id}", accountId)
                .headers(headers -> authorize(headers, userId)), AccountResponse.class);
    }

    public AccountResponse update(UUID userId, UUID accountId, UpdateAccountRequest request) {
        return execute(webClient.patch()
                .uri("/internal/accounts/{id}", accountId)
                .headers(headers -> authorize(headers, userId))
                .bodyValue(request), AccountResponse.class);
    }

    public void delete(UUID userId, UUID accountId) {
        execute(webClient.delete()
                .uri("/internal/accounts/{id}", accountId)
                .headers(headers -> authorize(headers, userId)), Void.class);
    }

    public BalanceResponse adjustBalance(UUID userId, UUID accountId, long deltaMinor) {
        return execute(webClient.post()
                .uri("/internal/accounts/{id}/balance-adjustments", accountId)
                .headers(headers -> authorize(headers, userId))
                .bodyValue(new BalanceAdjustmentRequest(deltaMinor)), BalanceResponse.class);
    }

    private void authorize(org.springframework.http.HttpHeaders headers, UUID userId) {
        headers.setBearerAuth(internalJwt.sign(userId));
    }

    private <T> T execute(
            WebClient.RequestHeadersSpec<?> request, Class<T> responseType) {
        return request.exchangeToMono(response -> handle(response, responseType)).block();
    }

    private <T> Mono<T> handle(ClientResponse response, Class<T> responseType) {
        HttpStatusCode status = response.statusCode();
        if (status.isError()) {
            return response.bodyToMono(ProblemDetail.class)
                    .defaultIfEmpty(ProblemDetail.forStatus(status))
                    .flatMap(problem -> Mono.error(new DownstreamException(status, problem)));
        }
        return response.bodyToMono(responseType);
    }
}
