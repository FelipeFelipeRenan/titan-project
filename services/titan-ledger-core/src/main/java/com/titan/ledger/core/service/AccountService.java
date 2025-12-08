package com.titan.ledger.core.service;

import java.util.Optional;

import org.springframework.stereotype.Service;

import com.titan.ledger.adapter.out.persistence.AccountRepository;
import com.titan.ledger.core.domain.model.Account;
import com.titan.ledger.core.usecase.CreateAccountUseCase;
import com.titan.ledger.core.usecase.dto.AccountResponse;
import com.titan.ledger.core.usecase.dto.CreateAccountCommand;

import jakarta.transaction.Transactional;

@Service
public class AccountService implements CreateAccountUseCase {

    private final AccountRepository accountRepository;

    public AccountService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Override
    @Transactional
    public AccountResponse execute(CreateAccountCommand command) {

        Optional<Account> existing = accountRepository.findByClientIdAndCurrency(
                command.clientId(),
                command.currency());

        if (existing.isPresent()) {
            return mapToResponse(existing.get(), "EXISTING");
        }

        Account newAccount = new Account(command.clientId(), command.currency());

        Account savedAccount = accountRepository.save(newAccount);

        return mapToResponse(savedAccount, "CREATED");

    }

    private AccountResponse mapToResponse(Account account, String status) {
        return new AccountResponse(
                account.getId(),
                account.getClientId(),
                account.getCurrency(),
                account.getBalance(),
                status);
    }
}
