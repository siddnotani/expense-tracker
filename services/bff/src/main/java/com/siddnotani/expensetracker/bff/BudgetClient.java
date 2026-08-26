package com.siddnotani.expensetracker.bff;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

import static com.siddnotani.expensetracker.bff.BffDtos.Budget;
import static com.siddnotani.expensetracker.bff.BffDtos.Category;

public class BudgetClient {
    private static final ParameterizedTypeReference<List<Budget>> BUDGETS =
            new ParameterizedTypeReference<>() {
            };
    private static final ParameterizedTypeReference<List<Category>> CATEGORIES =
            new ParameterizedTypeReference<>() {
            };

    private final WebClient webClient;
    private final CurrentSessionUser currentSessionUser;
    private final com.siddnotani.expensetracker.shared.auth.InternalJwt internalJwt;

    public BudgetClient(
            WebClient webClient,
            CurrentSessionUser currentSessionUser,
            com.siddnotani.expensetracker.shared.auth.InternalJwt internalJwt) {
        this.webClient = webClient;
        this.currentSessionUser = currentSessionUser;
        this.internalJwt = internalJwt;
    }

    public Mono<List<Budget>> budgets(String period) {
        return mapErrors(request(webClient.get().uri(uri -> uri.path("/internal/budgets")
                        .queryParam("period", period).build()))
                .retrieve().bodyToMono(BUDGETS));
    }

    public Mono<List<Category>> categories(boolean includeArchived) {
        return mapErrors(request(webClient.get().uri(uri -> uri.path("/internal/categories")
                        .queryParam("includeArchived", includeArchived).build()))
                .retrieve().bodyToMono(CATEGORIES));
    }

    public Mono<List<Category>> defaults() {
        return mapErrors(request(webClient.post().uri("/internal/categories/defaults"))
                .retrieve().bodyToMono(CATEGORIES));
    }

    public Mono<Category> createCategory(BffDtos.CreateCategoryRequest body) {
        return mapErrors(request(webClient.post().uri("/internal/categories")
                        .contentType(MediaType.APPLICATION_JSON).bodyValue(body))
                .retrieve().bodyToMono(Category.class));
    }

    public Mono<Category> updateCategory(UUID id, BffDtos.UpdateCategoryRequest body) {
        return mapErrors(request(webClient.patch().uri("/internal/categories/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON).bodyValue(body))
                .retrieve().bodyToMono(Category.class));
    }

    public Mono<Void> archiveCategory(UUID id) {
        return mapErrors(request(webClient.delete().uri("/internal/categories/{id}", id))
                .retrieve().bodyToMono(Void.class));
    }

    public Mono<Budget> upsertBudget(BffDtos.UpsertBudgetRequest body) {
        return mapErrors(request(webClient.put().uri("/internal/budgets")
                        .contentType(MediaType.APPLICATION_JSON).bodyValue(body))
                .retrieve().bodyToMono(Budget.class));
    }

    public Mono<Void> deleteBudget(UUID id) {
        return mapErrors(request(webClient.delete().uri("/internal/budgets/{id}", id))
                .retrieve().bodyToMono(Void.class));
    }

    public Mono<Void> copyBudgets(BffDtos.CopyBudgetsRequest body) {
        return mapErrors(request(webClient.post().uri("/internal/budgets/copy")
                        .contentType(MediaType.APPLICATION_JSON).bodyValue(body))
                .retrieve().bodyToMono(Void.class));
    }

    private WebClient.RequestHeadersSpec<?> request(WebClient.RequestHeadersSpec<?> request) {
        return request.headers(headers -> headers.setBearerAuth(
                internalJwt.sign(currentSessionUser.getId())));
    }

    private <T> Mono<T> mapErrors(Mono<T> mono) {
        return mono.onErrorMap(
                exception -> !(exception instanceof BffDownstreamException),
                BffDownstreamException::from);
    }
}
