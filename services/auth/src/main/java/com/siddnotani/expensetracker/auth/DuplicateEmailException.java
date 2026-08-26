package com.siddnotani.expensetracker.auth;

class DuplicateEmailException extends RuntimeException {
    DuplicateEmailException() {
        super("An account with this email already exists");
    }
}
