package com.titan.ledger.core.service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.titan.ledger.adapter.out.persistence.AccountRepository;
import com.titan.ledger.core.domain.exception.AccountNotFoundException;
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
                .orElseThrow(() -> new AccountNotFoundException("Account not found"));
    }


    public List<AccountResponse> listAll(){
        return accountRepository.findAll()
            .stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
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
