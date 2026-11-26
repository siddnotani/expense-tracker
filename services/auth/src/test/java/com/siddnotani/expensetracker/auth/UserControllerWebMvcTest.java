package com.siddnotani.expensetracker.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.siddnotani.expensetracker.shared.error.GlobalExceptionHandler;
import com.siddnotani.expensetracker.shared.error.NotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@Import({AuthExceptionHandler.class, GlobalExceptionHandler.class, AuthSecurityConfiguration.class})
class UserControllerWebMvcTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @Test
    void registerReturnsUserWithoutPasswordHash() throws Exception {
        UserEntity user = new UserEntity("alice@example.com", "$2a$12$not-returned", "Alice");
        when(userService.create(any())).thenReturn(user);

        mockMvc.perform(post("/internal/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"alice@example.com","password":"a-long-enough-password","displayName":"Alice"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("password_hash"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("$2a$"))));
    }

    @Test
    void duplicateEmailReturnsConflict() throws Exception {
        when(userService.create(any())).thenThrow(new DuplicateEmailException());

        mockMvc.perform(post("/internal/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"alice@example.com","password":"a-long-enough-password","displayName":"Alice"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "An account with this email already exists")));
    }

    @Test
    void authenticateSuccessReturnsUser() throws Exception {
        UserEntity user = new UserEntity("alice@example.com", "hash", "Alice");
        when(userService.authenticate(any())).thenReturn(user);

        mockMvc.perform(post("/internal/users/authenticate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"alice@example.com","password":"a-long-enough-password"}
                                """))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("alice@example.com")));
    }

    @Test
    void failedAuthenticationReturnsUnauthorized() throws Exception {
        when(userService.authenticate(any())).thenThrow(new AuthenticationFailedException());

        for (String email : new String[] {"alice@example.com", "unknown@example.com"}) {
            mockMvc.perform(post("/internal/users/authenticate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new AuthenticateRequest(email, "wrong-password"))))
                    .andExpect(status().isUnauthorized())
                    .andExpect(content().string(org.hamcrest.Matchers.not(
                            org.hamcrest.Matchers.containsString("$2a$"))));
        }
    }

    @Test
    void registrationValidationReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/internal/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"not-an-email","password":"short","displayName":" "}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void passwordOverBcryptLimitReturnsBadRequest() throws Exception {
        for (String password : new String[] {"a".repeat(73), "é".repeat(37)}) {
            mockMvc.perform(post("/internal/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new CreateUserRequest("alice@example.com", password, "Alice"))))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().string(org.hamcrest.Matchers.containsString("at most 72 bytes")));
        }
    }

    @Test
    void unknownUserReturnsNotFound() throws Exception {
        when(userService.get(any())).thenThrow(new NotFoundException("User not found"));

        mockMvc.perform(get("/internal/users/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }
}
