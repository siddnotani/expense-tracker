package com.siddnotani.expensetracker.accounts;

import com.siddnotani.expensetracker.shared.error.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class AccountService {
    private final AccountRepository accountRepository;
    private final AccountBalanceUpdater balanceUpdater;

    AccountService(AccountRepository accountRepository, AccountBalanceUpdater balanceUpdater) {
        this.accountRepository = accountRepository;
        this.balanceUpdater = balanceUpdater;
    }

    @Transactional(readOnly = true)
    public List<AccountResponse> list(UUID userId, boolean includeArchived) {
        List<Account> accounts = includeArchived
                ? accountRepository.findByUserIdOrderByCreatedAtAsc(userId)
                : accountRepository.findByUserIdAndArchivedFalseOrderByCreatedAtAsc(userId);
        return accounts.stream().map(AccountResponse::from).toList();
    }

    @Transactional
    public AccountResponse create(UUID userId, CreateAccountRequest request) {
        Account account = new Account(
                userId,
                request.name(),
                request.type(),
                request.currency(),
                request.openingBalanceMinor());
        return AccountResponse.from(accountRepository.save(account));
    }

    @Transactional(readOnly = true)
    public AccountResponse get(UUID userId, UUID id) {
        return AccountResponse.from(findOwned(userId, id));
    }

    @Transactional
    public AccountResponse update(UUID userId, UUID id, UpdateAccountRequest request) {
        Account account = findOwned(userId, id);
        if (request.name() != null) {
            account.setName(request.name());
        }
        if (request.archived() != null) {
            account.setArchived(request.archived());
        }
        return AccountResponse.from(account);
    }

    @Transactional
    public void archive(UUID userId, UUID id) {
        findOwned(userId, id).setArchived(true);
    }

    @Transactional
    public BalanceResponse adjustBalance(UUID userId, UUID id, long deltaMinor) {
        long balance = balanceUpdater.adjust(id, userId, deltaMinor)
                .orElseThrow(() -> new NotFoundException("Account not found"));
        return new BalanceResponse(id, balance);
    }

    private Account findOwned(UUID userId, UUID id) {
        return accountRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new NotFoundException("Account not found"));
    }
}
