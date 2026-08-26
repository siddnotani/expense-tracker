package com.siddnotani.expensetracker.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

record CreateUserRequest(
        @Email @NotBlank String email,
        @Size(min = 12) String password,
        @NotBlank String displayName) {
}

record AuthenticateRequest(String email, String password) {
}
