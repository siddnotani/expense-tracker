package com.siddnotani.expensetracker.bff.api;

import com.siddnotani.expensetracker.bff.CurrentSessionUser;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
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

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/transactions")
public class TransactionsApiController {
    private final TransactionGateway transactionGateway;
    private final TransactionBalanceCoordinator balanceCoordinator;
    private final CurrentSessionUser currentSessionUser;

    TransactionsApiController(
            TransactionGateway transactionGateway,
            TransactionBalanceCoordinator balanceCoordinator,
            CurrentSessionUser currentSessionUser) {
        this.transactionGateway = transactionGateway;
        this.balanceCoordinator = balanceCoordinator;
        this.currentSessionUser = currentSessionUser;
    }

    @GetMapping
    public TransactionPageResponse list(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate to,
            @RequestParam(required = false) UUID accountId,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) String direction,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return transactionGateway.list(
                currentSessionUser.getId(), from, to, accountId, categoryId, direction, page, size);
    }

    @PostMapping
    public ResponseEntity<TransactionResponse> create(
            @Valid @RequestBody TransactionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(balanceCoordinator.create(currentSessionUser.getId(), request));
    }

    @GetMapping("/{id}")
    public TransactionResponse get(@PathVariable UUID id) {
        return transactionGateway.get(currentSessionUser.getId(), id);
    }

    @PutMapping("/{id}")
    public TransactionResponse update(
            @PathVariable UUID id, @Valid @RequestBody TransactionRequest request) {
        return balanceCoordinator.update(currentSessionUser.getId(), id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        balanceCoordinator.delete(currentSessionUser.getId(), id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/summary")
    public SummaryResponse summary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate to) {
        return transactionGateway.summary(currentSessionUser.getId(), from, to);
    }
}
