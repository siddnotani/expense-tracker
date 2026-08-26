package com.siddnotani.expensetracker.auth;

class AuthenticationFailedException extends RuntimeException {
    AuthenticationFailedException() {
        super("Invalid email or password");
    }
}
