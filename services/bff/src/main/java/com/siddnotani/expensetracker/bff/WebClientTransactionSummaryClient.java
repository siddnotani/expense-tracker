package com.siddnotani.expensetracker.bff;

import com.siddnotani.expensetracker.shared.auth.InternalJwt;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDate;

@Component
public class WebClientTransactionSummaryClient implements TransactionSummaryClient {
    private final WebClient webClient;
    private final CurrentSessionUser currentSessionUser;
    private final InternalJwt internalJwt;

    public WebClientTransactionSummaryClient(
            @Qualifier("transactionWebClient") WebClient webClient,
            CurrentSessionUser currentSessionUser,
            InternalJwt internalJwt) {
        this.webClient = webClient;
        this.currentSessionUser = currentSessionUser;
        this.internalJwt = internalJwt;
    }

    @Override
    public Mono<BffDtos.TransactionSummary> summary(LocalDate from, LocalDate to) {
        return webClient.get()
                .uri(uri -> uri.path("/internal/transactions/summary")
                        .queryParam("from", from).queryParam("to", to).build())
                .headers(headers -> headers.setBearerAuth(
                        internalJwt.sign(currentSessionUser.getId())))
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(BffDtos.TransactionSummary.class)
                .onErrorMap(
                        exception -> !(exception instanceof BffDownstreamException),
                        BffDownstreamException::from);
    }
}
