package com.siddnotani.expensetracker.shared.security;

import com.siddnotani.expensetracker.shared.error.GlobalExceptionHandler;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Configuration
@org.springframework.context.annotation.Import(GlobalExceptionHandler.class)
public class SharedWebConfiguration {
    public static final String REQUEST_ID = "requestId";

    @Bean
    OncePerRequestFilter requestIdFilter() {
        return new OncePerRequestFilter() {
            @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
                String requestId = request.getHeader("X-Request-Id");
                if (requestId == null || requestId.isBlank()) requestId = UUID.randomUUID().toString();
                MDC.put(REQUEST_ID, requestId); response.setHeader("X-Request-Id", requestId);
                try { chain.doFilter(request, response); } finally { MDC.remove(REQUEST_ID); }
            }
        };
    }
}
