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
import com.titan.ledger.core.usecase.TransferFundsUseCase;
import com.titan.ledger.core.usecase.dto.TransferFundsCommand;

@Service
public class TransferService implements TransferFundsUseCase {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final LedgerRepository ledgerRepository;

    public TransferService(AccountRepository accountRepository, TransactionRepository transactionRepository,
            LedgerRepository ledgerRepository) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.ledgerRepository = ledgerRepository;
    }

    @Override
    @Transactional
    public UUID execute(TransferFundsCommand command) {

        // Carregar as contas
        // TODO: Usar lock perssimista para evitar possivel race condition
        Account fromAccount = accountRepository.findById(command.fromAccountId())
                .orElseThrow(() -> new IllegalArgumentException("Source account not found"));

        Account toAccount = accountRepository.findById(command.toAccountId())
                .orElseThrow(() -> new IllegalArgumentException("Target account not found"));

        // Validar saldo da conta que envia
        if (fromAccount.getBalance().compareTo(command.amount()) < 0) {
            throw new IllegalStateException("Insufficient funds");
        }

        // Criar header da transação
        Transaction transaction = new Transaction(
                UUID.randomUUID().toString(), // Correlation ID
                command.description());

        transaction.setStatus(TransactionStatus.COMPLETED); // Status ja completo, pois no momento é sincrono
        transactionRepository.save(transaction);

        // Lógica de negocio: atualizar saldos em memoria
        BigDecimal newSourceBalance = fromAccount.getBalance().subtract(command.amount());
        BigDecimal newTargetBalance = toAccount.getBalance().add(command.amount());

        fromAccount.setBalance(newSourceBalance);
        toAccount.setBalance(newTargetBalance);

        // salvar contas atualizadas
        accountRepository.save(fromAccount);
        accountRepository.save(toAccount);
        // Livro razao (ledger)

        // Entrada de debito
        LedgerEntry debitEntry = new LedgerEntry(
                transaction,
                fromAccount,
                OperationType.DEBIT,
                command.amount(),
                newSourceBalance // snapshot do saldo pós operação
        );

        // Entrada de credito
        LedgerEntry creditEntry = new LedgerEntry(
                transaction,
                toAccount,
                OperationType.CREDIT,
                command.amount(),
                newTargetBalance // snapshot do saldo pós operação
        );

        ledgerRepository.save(debitEntry);
        ledgerRepository.save(creditEntry);

        return transaction.getId();

    }

}
