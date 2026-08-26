package com.siddnotani.expensetracker.shared.security;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class CurrentUser {
    public UUID getId() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return UUID.fromString(principal.toString());
    }
}
