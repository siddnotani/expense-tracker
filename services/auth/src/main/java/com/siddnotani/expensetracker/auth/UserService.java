package com.siddnotani.expensetracker.auth;

import com.siddnotani.expensetracker.shared.error.NotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;

@Service
class UserService {
    private static final String DUMMY_PASSWORD = "dummy-password-for-timing";

    private final UserRepository repository;
    private final PasswordEncoder encoder;
    private final String dummyHash;

    UserService(UserRepository repository, PasswordEncoder encoder) {
        this.repository = repository;
        this.encoder = encoder;
        this.dummyHash = encoder.encode(DUMMY_PASSWORD);
    }

    @Transactional
    UserEntity create(CreateUserRequest request) {
        String email = normalizeEmail(request.email());
        if (repository.findByEmail(email).isPresent()) {
            throw new DuplicateEmailException();
        }
        try {
            return repository.saveAndFlush(
                    new UserEntity(email, encoder.encode(request.password()), request.displayName()));
        } catch (DataIntegrityViolationException exception) {
            throw new DuplicateEmailException();
        }
    }

    UserEntity authenticate(AuthenticateRequest request) {
        String email = normalizeEmail(request.email());
        UserEntity user = repository.findByEmail(email).orElse(null);
        String hash = user == null ? dummyHash : user.getPasswordHash();
        boolean matches = encoder.matches(request.password(), hash);
        if (user == null || !matches) {
            throw new AuthenticationFailedException();
        }
        return user;
    }

    UserEntity get(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found"));
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }
}
