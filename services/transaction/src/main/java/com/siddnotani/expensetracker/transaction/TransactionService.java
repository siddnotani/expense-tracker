package com.siddnotani.expensetracker.transaction;

import com.siddnotani.expensetracker.shared.error.NotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class TransactionService {
    private final TransactionRepository transactionRepository;
    private final TransactionSummaryQuery summaryQuery;

    TransactionService(TransactionRepository transactionRepository, TransactionSummaryQuery summaryQuery) {
        this.transactionRepository = transactionRepository;
        this.summaryQuery = summaryQuery;
    }

    @Transactional(readOnly = true)
    public TransactionPageResponse list(
            UUID userId,
            LocalDate from,
            LocalDate to,
            UUID accountId,
            UUID categoryId,
            Direction direction,
            int page,
            int size) {
        int normalizedPage = Math.max(page, 0);
        int normalizedSize = Math.min(Math.max(size, 1), 200);
        PageRequest pageable = PageRequest.of(
                normalizedPage,
                normalizedSize,
                Sort.by(Sort.Order.desc("occurredOn"), Sort.Order.desc("createdAt")));
        Page<Transaction> result = transactionRepository.findAll(
                TransactionSpecifications.filtered(userId, from, to, accountId, categoryId, direction),
                pageable);
        List<TransactionResponse> items = result.getContent().stream()
                .map(TransactionResponse::from)
                .toList();
        return new TransactionPageResponse(items, result.getTotalElements(), normalizedPage, normalizedSize);
    }

    @Transactional
    public TransactionResponse create(UUID userId, TransactionRequest request) {
        Transaction transaction = new Transaction(
                userId,
                request.accountId(),
                request.categoryId(),
                request.direction(),
                request.amountMinor(),
                request.currency(),
                request.occurredOn(),
                request.description());
        return TransactionResponse.from(transactionRepository.save(transaction));
    }

    @Transactional(readOnly = true)
    public TransactionResponse get(UUID userId, UUID id) {
        return TransactionResponse.from(findOwned(userId, id));
    }

    @Transactional
    public TransactionResponse update(UUID userId, UUID id, TransactionRequest request) {
        Transaction transaction = findOwned(userId, id);
        transaction.update(
                request.accountId(),
                request.categoryId(),
                request.direction(),
                request.amountMinor(),
                request.currency(),
                request.occurredOn(),
                request.description());
        return TransactionResponse.from(transaction);
    }

    @Transactional
    public void delete(UUID userId, UUID id) {
        transactionRepository.delete(findOwned(userId, id));
    }

    @Transactional(readOnly = true)
    public SummaryResponse summary(UUID userId, LocalDate from, LocalDate to) {
        return summaryQuery.summarize(userId, from, to);
    }

    private Transaction findOwned(UUID userId, UUID id) {
        return transactionRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new NotFoundException("Transaction not found"));
    }
}
