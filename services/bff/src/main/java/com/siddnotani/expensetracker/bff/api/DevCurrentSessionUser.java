package com.siddnotani.expensetracker.bff.api;

import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.UUID;

/**
 * TEMPORARY-SEAM: resolves the caller from the {@code X-User-Id} header, falling back to a fixed
 * development user, so the API can be exercised before the session cookie exists.
 *
 * <p>The sibling auth/session PR owns the real implementation of {@link CurrentSessionUser}, which
 * reads the authenticated principal from the security context. Delete this class when that PR is
 * joined to this one.
 */
@Component
public class DevCurrentSessionUser implements CurrentSessionUser {
    public static final UUID DEV_USER_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Override
    public UUID getId() {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            UUID headerId = parse(attributes.getRequest().getHeader("X-User-Id"));
            if (headerId != null) {
                return headerId;
            }
        }
        return DEV_USER_ID;
    }

    private UUID parse(String value) {
        if (value == null) {
            return null;
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }
}
