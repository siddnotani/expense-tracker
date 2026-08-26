package com.siddnotani.expensetracker.accounts;

import com.siddnotani.expensetracker.shared.security.CurrentUser;
import jakarta.validation.Valid;
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

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/internal/accounts")
public class AccountController {
    private final AccountService accountService;
    private final CurrentUser currentUser;

    AccountController(AccountService accountService, CurrentUser currentUser) {
        this.accountService = accountService;
        this.currentUser = currentUser;
    }

    @GetMapping
    List<AccountResponse> list(
            @RequestParam(defaultValue = "false") boolean includeArchived) {
        return accountService.list(currentUser.getId(), includeArchived);
    }

    @PostMapping
    ResponseEntity<AccountResponse> create(@Valid @RequestBody CreateAccountRequest request) {
        AccountResponse response = accountService.create(currentUser.getId(), request);
        return ResponseEntity.created(URI.create("/internal/accounts/" + response.id()))
                .body(response);
    }

    @GetMapping("/{id}")
    AccountResponse get(@PathVariable UUID id) {
        return accountService.get(currentUser.getId(), id);
    }

    @PatchMapping("/{id}")
    AccountResponse update(
            @PathVariable UUID id, @Valid @RequestBody UpdateAccountRequest request) {
        return accountService.update(currentUser.getId(), id, request);
    }

    @DeleteMapping("/{id}")
    ResponseEntity<Void> delete(@PathVariable UUID id) {
        accountService.archive(currentUser.getId(), id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/balance-adjustments")
    BalanceResponse adjustBalance(
            @PathVariable UUID id, @Valid @RequestBody BalanceAdjustmentRequest request) {
        return accountService.adjustBalance(currentUser.getId(), id, request.deltaMinor());
    }
}
