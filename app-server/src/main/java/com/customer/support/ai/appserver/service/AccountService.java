package com.customer.support.ai.appserver.service;

import com.customer.support.ai.appserver.dto.AccountDetailResponse;
import com.customer.support.ai.appserver.dto.AccountUpdateRequest;
import com.customer.support.ai.appserver.dto.CursorPage;
import com.customer.support.ai.appserver.dto.CursorSupport;
import com.customer.support.ai.appserver.dto.TransactionListItem;
import com.customer.support.ai.appserver.entity.Account;
import com.customer.support.ai.appserver.entity.Transaction;
import com.customer.support.ai.appserver.exception.EntityNotFoundException;
import com.customer.support.ai.appserver.repository.AccountRepository;
import com.customer.support.ai.appserver.repository.TransactionRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    public AccountService(AccountRepository accountRepository, TransactionRepository transactionRepository) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
    }

    public AccountDetailResponse getById(UUID id) {
        return AccountDetailResponse.from(findAccount(id));
    }

    @Transactional
    public AccountDetailResponse update(UUID id, AccountUpdateRequest request) {
        Account account = findAccount(id);
        account.setAccountStatus(request.accountStatus());
        account.setRiskRating(request.riskRating());
        return AccountDetailResponse.from(account);
    }

    public CursorPage<TransactionListItem> getTransactions(UUID accountId, String cursor, int size) {
        if (!accountRepository.existsById(accountId)) {
            throw new EntityNotFoundException("Account not found: " + accountId);
        }
        UUID cursorId = CursorSupport.decode(cursor);
        Limit limit = Limit.of(size + 1);
        List<Transaction> rows = cursorId == null
                ? transactionRepository.findByAccountIdOrderByIdDesc(accountId, limit)
                : transactionRepository.findByAccountIdAndIdLessThanOrderByIdDesc(accountId, cursorId, limit);
        long total = transactionRepository.countByAccountId(accountId);
        return CursorSupport.build(rows, size, total, Transaction::getId, TransactionListItem::from);
    }

    private Account findAccount(UUID id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Account not found: " + id));
    }
}
