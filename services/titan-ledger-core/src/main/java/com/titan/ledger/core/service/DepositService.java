package com.titan.ledger.core.service;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.titan.ledger.adapter.out.persistence.AccountRepository;
import com.titan.ledger.adapter.out.persistence.LedgerRepository;
import com.titan.ledger.adapter.out.persistence.TransactionRepository;
import com.titan.ledger.core.domain.model.Account;
import com.titan.ledger.core.domain.model.LedgerEntry;
import com.titan.ledger.core.domain.model.OperationType;
import com.titan.ledger.core.domain.model.Transaction;
import com.titan.ledger.core.domain.model.TransactionStatus;
import com.titan.ledger.core.usecase.DepositUseCase;
import com.titan.ledger.core.usecase.dto.DepositCommand;

@Service
public class DepositService implements DepositUseCase{
    
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final LedgerRepository ledgerRepository;

    
    public DepositService(AccountRepository accountRepository, TransactionRepository transactionRepository,
            LedgerRepository ledgerRepository) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.ledgerRepository = ledgerRepository;
    }


    @Override
    @Transactional
    public void execute(DepositCommand command) {
        // Buscar conta
        Account account = accountRepository.findById(command.accountId())
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));
        
        // Criar transação
        Transaction transaction = new Transaction(
            UUID.randomUUID().toString(),
            command.description() != null ? command.description() : "Cash-in"
        );
        transaction.setStatus(TransactionStatus.COMPLETED);
        transactionRepository.save(transaction);

        // Atualiza o saldo
        BigDecimal newBalance = account.getBalance().add(command.amount());

        account.setBalance(newBalance);
        accountRepository.save(account);

        // grava no ledger como credito
        LedgerEntry ledgerEntry = new LedgerEntry(
            transaction,
            account,
            OperationType.CREDIT,
            command.amount(),
            newBalance
        );
        ledgerRepository.save(ledgerEntry);
    }
}
