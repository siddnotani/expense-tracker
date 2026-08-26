package com.siddnotani.expensetracker.bff.api;

import com.siddnotani.expensetracker.shared.auth.InternalJwt;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.UUID;

@Component
public class TransactionGateway {
    private final WebClient webClient;
    private final InternalJwt internalJwt;

    TransactionGateway(
            @Qualifier("transactionWebClient") WebClient webClient,
            InternalJwt internalJwt) {
        this.webClient = webClient;
        this.internalJwt = internalJwt;
    }

    public TransactionPageResponse list(
            UUID userId,
            LocalDate from,
            LocalDate to,
            UUID accountId,
            UUID categoryId,
            String direction,
            int page,
            int size) {
        return execute(webClient.get()
                .uri(uri -> {
                    var builder = uri.path("/internal/transactions")
                            .queryParam("page", page)
                            .queryParam("size", size);
                    if (from != null) {
                        builder.queryParam("from", from);
                    }
                    if (to != null) {
                        builder.queryParam("to", to);
                    }
                    if (accountId != null) {
                        builder.queryParam("accountId", accountId);
                    }
                    if (categoryId != null) {
                        builder.queryParam("categoryId", categoryId);
                    }
                    if (direction != null) {
                        builder.queryParam("direction", direction);
                    }
                    return builder.build();
                })
                .headers(headers -> authorize(headers, userId)), TransactionPageResponse.class);
    }

    public TransactionResponse create(UUID userId, TransactionRequest request) {
        return execute(webClient.post()
                .uri("/internal/transactions")
                .headers(headers -> authorize(headers, userId))
                .bodyValue(request), TransactionResponse.class);
    }

    public TransactionResponse get(UUID userId, UUID transactionId) {
        return execute(webClient.get()
                .uri("/internal/transactions/{id}", transactionId)
                .headers(headers -> authorize(headers, userId)), TransactionResponse.class);
    }

    public TransactionResponse update(
            UUID userId, UUID transactionId, TransactionRequest request) {
        return execute(webClient.put()
                .uri("/internal/transactions/{id}", transactionId)
                .headers(headers -> authorize(headers, userId))
                .bodyValue(request), TransactionResponse.class);
    }

    public void delete(UUID userId, UUID transactionId) {
        execute(webClient.delete()
                .uri("/internal/transactions/{id}", transactionId)
                .headers(headers -> authorize(headers, userId)), Void.class);
    }

    public SummaryResponse summary(UUID userId, LocalDate from, LocalDate to) {
        return execute(webClient.get()
                .uri(uri -> {
                    var builder = uri.path("/internal/transactions/summary");
                    if (from != null) {
                        builder.queryParam("from", from);
                    }
                    if (to != null) {
                        builder.queryParam("to", to);
                    }
                    return builder.build();
                })
                .headers(headers -> authorize(headers, userId)), SummaryResponse.class);
    }

    private void authorize(HttpHeaders headers, UUID userId) {
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
