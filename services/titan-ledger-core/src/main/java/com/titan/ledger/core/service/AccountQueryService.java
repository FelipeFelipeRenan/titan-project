package com.titan.ledger.core.service;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.titan.ledger.adapter.out.persistence.AccountRepository;
import com.titan.ledger.core.domain.model.Account;
import com.titan.ledger.core.usecase.GetAccountBalanceUseCase;
import com.titan.ledger.core.usecase.dto.AccountResponse;


@Service
@Transactional(readOnly = true)
public class AccountQueryService implements GetAccountBalanceUseCase {


    private final AccountRepository accountRepository;

    
    public AccountQueryService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }


    @Override
    public AccountResponse execute(UUID accountId) {
        // Using standard repo, for now

        return accountRepository.findById(accountId)
                .map(this::mapToResponse)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));
    }
    
    private AccountResponse mapToResponse(Account account) {
        return new AccountResponse(
            account.getId(),
            account.getClientId(),
            account.getCurrency(),
            account.getBalance(),
            "ACTIVE"
        );
    }
}
