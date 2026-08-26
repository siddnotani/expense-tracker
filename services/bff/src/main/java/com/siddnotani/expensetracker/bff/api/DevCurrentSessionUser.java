package com.siddnotani.expensetracker.bff.api;

import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.UUID;

/**
 * TEMPORARY-SEAM: join this implementation to the sibling auth/session PR.
 *
 * <p>It supports local curl verification until the real session filter is available.</p>
 */
// TEMPORARY-SEAM: replace with the implementation from the sibling auth/session PR.
@Component
public class DevCurrentSessionUser implements CurrentSessionUser {
    public static final UUID DEV_USER_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Override
    public UUID getId() {
        UUID principalId = authenticatedPrincipalId();
        if (principalId != null) {
            return principalId;
        }

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

    private UUID authenticatedPrincipalId() {
        try {
            Class<?> holder = Class.forName(
                    "org.springframework.security.core.context.SecurityContextHolder");
            Object context = holder.getMethod("getContext").invoke(null);
            Object authentication = context.getClass().getMethod("getAuthentication").invoke(context);
            if (authentication == null
                    || !(Boolean) authentication.getClass().getMethod("isAuthenticated")
                            .invoke(authentication)) {
                return null;
            }
            Object principal = authentication.getClass().getMethod("getPrincipal").invoke(authentication);
            if (principal == null || "anonymousUser".equals(principal)) {
                return null;
            }
            return parse(principal.toString());
        } catch (ReflectiveOperationException | LinkageError exception) {
            return null;
        }
    }

    private UUID parse(String value) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }
}
