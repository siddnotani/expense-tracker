package com.siddnotani.expensetracker.bff;

import com.siddnotani.expensetracker.shared.auth.InternalJwt;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class BudgetClientConfiguration {
    @Bean
    BudgetClient budgetClient(
            @Qualifier("budgetWebClient") WebClient webClient,
            SessionUserProvider sessionUserProvider,
            InternalJwt internalJwt) {
        return new BudgetClient(webClient, sessionUserProvider, internalJwt);
    }
}
