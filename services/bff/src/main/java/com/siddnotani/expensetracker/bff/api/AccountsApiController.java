package com.siddnotani.expensetracker.bff.api;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/accounts")
public class AccountsApiController {
    private final AccountsGateway accountsGateway;
    private final CurrentSessionUser currentSessionUser;

    AccountsApiController(AccountsGateway accountsGateway, CurrentSessionUser currentSessionUser) {
        this.accountsGateway = accountsGateway;
        this.currentSessionUser = currentSessionUser;
    }

    @GetMapping
    public List<AccountResponse> list(
            @RequestParam(defaultValue = "false") boolean includeArchived) {
        return accountsGateway.list(currentSessionUser.getId(), includeArchived);
    }

    @PostMapping
    public ResponseEntity<AccountResponse> create(
            @Valid @RequestBody CreateAccountRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(accountsGateway.create(currentSessionUser.getId(), request));
    }

    @GetMapping("/{id}")
    public AccountResponse get(@PathVariable UUID id) {
        return accountsGateway.get(currentSessionUser.getId(), id);
    }

    @PatchMapping("/{id}")
    public AccountResponse update(
            @PathVariable UUID id, @Valid @RequestBody UpdateAccountRequest request) {
        return accountsGateway.update(currentSessionUser.getId(), id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        accountsGateway.delete(currentSessionUser.getId(), id);
        return ResponseEntity.noContent().build();
    }
}
