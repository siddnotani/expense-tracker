package com.siddnotani.expensetracker.transaction;

import com.siddnotani.expensetracker.shared.security.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/internal/transactions")
public class TransactionController {
    private final TransactionService transactionService;
    private final CurrentUser currentUser;

    TransactionController(TransactionService transactionService, CurrentUser currentUser) {
        this.transactionService = transactionService;
        this.currentUser = currentUser;
    }

    @GetMapping
    TransactionPageResponse list(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate to,
            @RequestParam(required = false) UUID accountId,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) Direction direction,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return transactionService.list(
                currentUser.getId(), from, to, accountId, categoryId, direction, page, size);
    }

    @PostMapping
    ResponseEntity<TransactionResponse> create(@Valid @RequestBody TransactionRequest request) {
        TransactionResponse response = transactionService.create(currentUser.getId(), request);
        return ResponseEntity.created(URI.create("/internal/transactions/" + response.id()))
                .body(response);
    }

    @GetMapping("/{id}")
    TransactionResponse get(@PathVariable UUID id) {
        return transactionService.get(currentUser.getId(), id);
    }

    @PutMapping("/{id}")
    TransactionResponse update(
            @PathVariable UUID id, @Valid @RequestBody TransactionRequest request) {
        return transactionService.update(currentUser.getId(), id, request);
    }

    @DeleteMapping("/{id}")
    ResponseEntity<Void> delete(@PathVariable UUID id) {
        transactionService.delete(currentUser.getId(), id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/summary")
    SummaryResponse summary(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return transactionService.summary(currentUser.getId(), from, to);
    }
}
