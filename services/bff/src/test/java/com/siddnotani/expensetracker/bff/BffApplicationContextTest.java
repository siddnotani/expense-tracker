package com.siddnotani.expensetracker.bff;

import com.siddnotani.expensetracker.shared.auth.InternalJwt;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class BffApplicationContextTest {
    @Autowired
    private InternalJwt internalJwt;

    @Test
    void contextLoads() {
    }

    @Test
    void internalJwtRoundTrips() {
        UUID userId = UUID.randomUUID();
        assertEquals(userId, internalJwt.verify(internalJwt.sign(userId)));
    }
}
