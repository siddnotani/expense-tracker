package com.siddnotani.expensetracker.shared.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Component
public class InternalJwt {
    private static final Duration TTL = Duration.ofMinutes(5);
    private final SecretKey key;

    public InternalJwt(@Value("${internal.jwt.secret}") String secret) {
        if (secret == null || secret.length() < 32) {
            throw new IllegalArgumentException("internal.jwt.secret must contain at least 32 characters");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String sign(UUID userId) {
        Instant now = Instant.now();
        return Jwts.builder().subject(userId.toString()).expiration(Date.from(now.plus(TTL)))
                .signWith(key).compact();
    }

    public UUID verify(String token) {
        Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        if (claims.size() != 2 || claims.getSubject() == null || claims.getExpiration() == null) {
            throw new IllegalArgumentException("internal JWT must contain only sub and exp claims");
        }
        return UUID.fromString(claims.getSubject());
    }
}
