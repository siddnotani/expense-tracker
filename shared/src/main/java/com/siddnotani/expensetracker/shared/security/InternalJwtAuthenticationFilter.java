package com.siddnotani.expensetracker.shared.security;

import com.siddnotani.expensetracker.shared.auth.InternalJwt;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

public class InternalJwtAuthenticationFilter extends OncePerRequestFilter {
    private final InternalJwt internalJwt;

    public InternalJwtAuthenticationFilter(InternalJwt internalJwt) { this.internalJwt = internalJwt; }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) { return request.getRequestURI().startsWith("/actuator/"); }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith("Bearer ")) { chain.doFilter(request, response); return; }
        try {
            UUID userId = internalJwt.verify(header.substring(7));
            var authentication = new UsernamePasswordAuthenticationToken(userId.toString(), null, java.util.List.of());
            SecurityContextHolder.getContext().setAuthentication(authentication);
            chain.doFilter(request, response);
        } catch (RuntimeException ex) {
            SecurityContextHolder.clearContext();
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid internal JWT");
        }
    }
}
