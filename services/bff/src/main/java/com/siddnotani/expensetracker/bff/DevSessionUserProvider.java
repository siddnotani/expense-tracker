package com.siddnotani.expensetracker.bff;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class DevSessionUserProvider implements SessionUserProvider {
    private final UUID userId;

    public DevSessionUserProvider(
            @Value("${bff.dev-user-id:00000000-0000-0000-0000-000000000001}") UUID userId) {
        this.userId = userId;
    }

    @Override
    public UUID currentUserId() {
        return userId;
    }
}
