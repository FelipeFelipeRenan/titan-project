package com.titan.ledger.core.service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.titan.ledger.adapter.out.persistence.AccountRepository;
import com.titan.ledger.adapter.out.persistence.LedgerRepository;
import com.titan.ledger.core.domain.exception.AccountNotFoundException;
import com.titan.ledger.core.domain.model.Account;
import com.titan.ledger.core.domain.model.LedgerEntry;
import com.titan.ledger.core.usecase.GetAccountBalanceUseCase;
import com.titan.ledger.core.usecase.dto.AccountResponse;
import com.titan.ledger.core.usecase.dto.StatementEntryResponse;


@Service
@Transactional(readOnly = true)
public class AccountQueryService implements GetAccountBalanceUseCase {


    private final AccountRepository accountRepository;
    private final LedgerRepository ledgerRepository;

    
    public AccountQueryService(AccountRepository accountRepository, LedgerRepository ledgerRepository) {
        this.accountRepository = accountRepository;
        this.ledgerRepository = ledgerRepository;
    }


    @Override
    @Cacheable(value = "account", key = "#accountId", unless = "#result == null")
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

    // extrato paginado
    @Cacheable(
        value = "statements",
        key = "#accountId + '::' + #pageable.pageNumber",
        condition = "pageable.pageNumber == 0",
        unless = "#result.isEmpty"
    )
    public Page<StatementEntryResponse> getStatement(UUID accountId, Pageable pageable){

        if (!accountRepository.existsById(accountId)) {
            throw new AccountNotFoundException("Account not found");
        }

        return ledgerRepository.findByAccount_Id(accountId, pageable)
            .map(this::mapToStatementResponse);
    }

    private StatementEntryResponse mapToStatementResponse(LedgerEntry entry) {
        return new StatementEntryResponse(
            entry.getTransaction().getId(),
            entry.getType().name(),
            entry.getAmount(),
            entry.getBalanceSnapshot(),
            entry.getTransaction().getDescription(),
            entry.getCreatedAt()
        );
    }

}
