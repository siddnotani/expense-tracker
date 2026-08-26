package com.siddnotani.expensetracker.bff;

import com.siddnotani.expensetracker.shared.auth.InternalJwt;
import org.springframework.boot.web.reactive.function.client.WebClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;

import java.util.UUID;

@Configuration
class InternalJwtWebClientCustomizer {
    @Bean
    WebClientCustomizer internalJwtFilter(InternalJwt internalJwt) {
        ExchangeFilterFunction filter = (request, next) -> {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null) {
                return next.exchange(request);
            }
            try {
                UUID userId = UUID.fromString(authentication.getName());
                ClientRequest authenticatedRequest = ClientRequest.from(request)
                        .headers(headers -> headers.setBearerAuth(internalJwt.sign(userId)))
                        .build();
                return next.exchange(authenticatedRequest);
            } catch (IllegalArgumentException exception) {
                return next.exchange(request);
            }
        };
        return builder -> builder.filter(filter);
    }
}
