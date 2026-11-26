package com.siddnotani.expensetracker.auth;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.nio.charset.StandardCharsets;

record CreateUserRequest(
        @Email @NotBlank String email,
        @Size(min = 12) String password,
        @NotBlank String displayName) {

    // BCrypt only uses the first 72 bytes and Spring Security 6.3.8+ rejects longer input.
    @AssertTrue(message = "password must be at most 72 bytes")
    boolean isPasswordWithinBcryptLimit() {
        return password == null || password.getBytes(StandardCharsets.UTF_8).length <= 72;
    }
}

record AuthenticateRequest(String email, String password) {
}
