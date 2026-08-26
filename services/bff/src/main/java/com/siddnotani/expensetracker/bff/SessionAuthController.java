package com.siddnotani.expensetracker.bff;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
class SessionAuthController {
    private static final String EMAIL_ATTRIBUTE = SessionAuthController.class.getName() + ".email";
    private static final String DISPLAY_NAME_ATTRIBUTE =
            SessionAuthController.class.getName() + ".displayName";

    private final AuthProxyService authProxyService;
    private final HttpSessionSecurityContextRepository securityContextRepository =
            new HttpSessionSecurityContextRepository();

    SessionAuthController(AuthProxyService authProxyService) {
        this.authProxyService = authProxyService;
    }

    @PostMapping("/register")
    UserResponse register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse) {
        AuthProxyService.UserResponse user = authProxyService.register(
                new AuthProxyService.RegisterRequest(request.email(), request.password(), request.displayName()));
        establishSession(user, servletRequest, servletResponse);
        return UserResponse.from(user);
    }

    @PostMapping("/login")
    UserResponse login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse) {
        AuthProxyService.UserResponse user = authProxyService.authenticate(
                new AuthProxyService.LoginRequest(request.email(), request.password()));
        establishSession(user, servletRequest, servletResponse);
        return UserResponse.from(user);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void logout(HttpServletRequest request) {
        SecurityContextHolder.clearContext();
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }

    @GetMapping("/me")
    UserResponse me(HttpServletRequest request) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            throw new IllegalStateException("no authenticated user");
        }
        UUID id = UUID.fromString(authentication.getName());
        HttpSession session = request.getSession(false);
        String email = session == null ? null : (String) session.getAttribute(EMAIL_ATTRIBUTE);
        String displayName =
                session == null ? null : (String) session.getAttribute(DISPLAY_NAME_ATTRIBUTE);
        if (email == null || displayName == null) {
            AuthProxyService.UserResponse user = authProxyService.get(id);
            if (user == null) {
                throw new IllegalStateException("authenticated user not found");
            }
            HttpSession restoredSession = request.getSession(true);
            restoredSession.setAttribute(EMAIL_ATTRIBUTE, user.email());
            restoredSession.setAttribute(DISPLAY_NAME_ATTRIBUTE, user.displayName());
            return UserResponse.from(user);
        }
        return new UserResponse(id, email, displayName);
    }

    private void establishSession(
            AuthProxyService.UserResponse user,
            HttpServletRequest request,
            HttpServletResponse response) {
        HttpSession session = request.getSession(true);
        request.changeSessionId();
        var authentication = new UsernamePasswordAuthenticationToken(
                user.id().toString(), null, List.of());
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        session.setAttribute(EMAIL_ATTRIBUTE, user.email());
        session.setAttribute(DISPLAY_NAME_ATTRIBUTE, user.displayName());
        securityContextRepository.saveContext(context, request, response);
    }

    record RegisterRequest(
            @Email @NotBlank String email,
            @Size(min = 12) String password,
            @NotBlank String displayName) {
    }

    record LoginRequest(@Email @NotBlank String email, @NotBlank String password) {
    }

    record UserResponse(UUID id, String email, String displayName) {
        static UserResponse from(AuthProxyService.UserResponse user) {
            return new UserResponse(user.id(), user.email(), user.displayName());
        }
    }
}
