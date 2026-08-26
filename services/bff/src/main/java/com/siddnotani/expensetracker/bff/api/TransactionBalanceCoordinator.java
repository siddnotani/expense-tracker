package com.siddnotani.expensetracker.bff.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import com.siddnotani.expensetracker.shared.security.SharedWebConfiguration;

import java.util.UUID;

@Component
public class TransactionBalanceCoordinator {
    private static final Logger LOGGER = LoggerFactory.getLogger(TransactionBalanceCoordinator.class);

    private final TransactionGateway transactionGateway;
    private final AccountsGateway accountsGateway;

    TransactionBalanceCoordinator(
            TransactionGateway transactionGateway, AccountsGateway accountsGateway) {
        this.transactionGateway = transactionGateway;
        this.accountsGateway = accountsGateway;
    }

    public TransactionResponse create(UUID userId, TransactionRequest request) {
        TransactionResponse transaction = transactionGateway.create(userId, request);
        adjustOrThrow(
                userId,
                transaction.id(),
                transaction.accountId(),
                delta(transaction.direction(), transaction.amountMinor()));
        return transaction;
    }

    public TransactionResponse update(
            UUID userId, UUID transactionId, TransactionRequest request) {
        TransactionResponse existing = transactionGateway.get(userId, transactionId);
        TransactionResponse updated = transactionGateway.update(userId, transactionId, request);
        long oldDelta = delta(existing.direction(), existing.amountMinor());
        long newDelta = delta(updated.direction(), updated.amountMinor());
        if (existing.accountId().equals(updated.accountId())) {
            adjustOrThrow(userId, updated.id(), updated.accountId(), newDelta - oldDelta);
        } else {
            adjustOrThrow(userId, updated.id(), existing.accountId(), -oldDelta);
            adjustOrThrow(userId, updated.id(), updated.accountId(), newDelta);
        }
        return updated;
    }

    public void delete(UUID userId, UUID transactionId) {
        TransactionResponse existing = transactionGateway.get(userId, transactionId);
        transactionGateway.delete(userId, transactionId);
        adjustOrThrow(
                userId,
                existing.id(),
                existing.accountId(),
                -delta(existing.direction(), existing.amountMinor()));
    }

    static long delta(String direction, long amountMinor) {
        return "EXPENSE".equals(direction) ? -amountMinor : amountMinor;
    }

    private void adjustOrThrow(UUID userId, UUID transactionId, UUID accountId, long deltaMinor) {
        try {
            accountsGateway.adjustBalance(userId, accountId, deltaMinor);
        } catch (RuntimeException exception) {
            String requestId = MDC.get(SharedWebConfiguration.REQUEST_ID);
            LOGGER.error(
                    "Balance synchronization failed requestId={} transactionId={} accountId={}",
                    requestId, transactionId, accountId, exception);
            throw new BalanceSyncException(transactionId, accountId, exception);
        }
    }
}
