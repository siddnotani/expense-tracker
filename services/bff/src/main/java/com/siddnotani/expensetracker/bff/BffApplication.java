package com.siddnotani.expensetracker.bff;

import com.siddnotani.expensetracker.shared.security.SharedWebConfiguration;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.UUID;

@SpringBootApplication
@Import({SharedWebConfiguration.class, BffSecurityConfiguration.class})
public class BffApplication {
    public static void main(String[] args) {
        SpringApplication.run(BffApplication.class, args);
    }

    @Bean
    WebClient authWebClient(
            WebClient.Builder builder, @Value("${downstream.auth-url}") String url) {
        return downstream(builder, url);
    }

    @Bean
    WebClient accountsWebClient(
            WebClient.Builder builder, @Value("${downstream.accounts-url}") String url) {
        return downstream(builder, url);
    }

    @Bean
    WebClient transactionWebClient(
            WebClient.Builder builder, @Value("${downstream.transaction-url}") String url) {
        return downstream(builder, url);
    }

    @Bean
    WebClient budgetWebClient(
            WebClient.Builder builder, @Value("${downstream.budget-url}") String url) {
        return downstream(builder, url);
    }

    private WebClient downstream(WebClient.Builder builder, String url) {
        return builder
                .clone()
                .baseUrl(url)
                .filter((request, next) -> {
                    String requestId = MDC.get(SharedWebConfiguration.REQUEST_ID);
                    ClientRequest forwarded = ClientRequest.from(request)
                            .header("X-Request-Id", requestId == null ? UUID.randomUUID().toString() : requestId)
                            .build();
                    return next.exchange(forwarded);
                })
                .build();
    }
}
