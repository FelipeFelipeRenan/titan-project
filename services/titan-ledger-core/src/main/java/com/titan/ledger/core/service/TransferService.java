package com.titan.ledger.core.service;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.titan.ledger.adapter.out.persistence.AccountRepository;
import com.titan.ledger.adapter.out.persistence.IdempotencyRepository;
import com.titan.ledger.adapter.out.persistence.LedgerRepository;
import com.titan.ledger.adapter.out.persistence.TransactionRepository;
import com.titan.ledger.core.domain.exception.AccountNotFoundException;
import com.titan.ledger.core.domain.exception.InsufficientFundsException;
import com.titan.ledger.core.domain.model.Account;
import com.titan.ledger.core.domain.model.IdempotencyKey;
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
        private final IdempotencyRepository idempotencyRespository;

        public TransferService(AccountRepository accountRepository, TransactionRepository transactionRepository,
                        LedgerRepository ledgerRepository, IdempotencyRepository idempotencyRespository) {
                this.accountRepository = accountRepository;
                this.transactionRepository = transactionRepository;
                this.ledgerRepository = ledgerRepository;
                this.idempotencyRespository = idempotencyRespository;
        }

        @Override
        @Transactional
        public UUID execute(TransferFundsCommand command) {

                // idempotency check
                if (command.idempotencyKey() != null) {
                        return idempotencyRespository.findById(command.idempotencyKey())
                                        .map(existing -> {
                                                // --- CORREÇÃO AQUI ---
                                                // O body agora é {"transactionId": "uuid"}, precisamos extrair o UUID.
                                                // Como não queremos adicionar biblioteca de JSON só pra isso agora,
                                                // vamos fazer um "parse de pedreiro" (substring) rápido.
                                                String body = existing.getResponseBody();
                                                String uuidString = body.replace("{\"transactionId\": \"", "")
                                                                .replace("\"}", "");
                                                return UUID.fromString(uuidString);
                                                // ---------------------
                                        })
                                        .orElseGet(() -> processNewTransfer(command));
                }

                return processNewTransfer(command);

        }

        private UUID processNewTransfer(TransferFundsCommand command) {

                // DEADLOCK PREVEMTION - ordenar locks por UUID
                UUID firstLockId = command.fromAccountId().compareTo(command.toAccountId()) < 0
                                ? command.fromAccountId()
                                : command.toAccountId();

                UUID secondLockId = command.fromAccountId().compareTo(command.toAccountId()) < 0
                                ? command.toAccountId()
                                : command.fromAccountId();

                // Carregar as contas
                Account account1 = accountRepository.findByIdForUpdate(firstLockId)
                                .orElseThrow(() -> new AccountNotFoundException("Source account not found"));

                Account account2 = accountRepository.findByIdForUpdate(secondLockId)
                                .orElseThrow(() -> new AccountNotFoundException("Target account not found"));

                Account fromAccount = command.fromAccountId().equals(account1.getId()) ? account1 : account2;

                Account toAccount = command.toAccountId().equals(account1.getId()) ? account1 : account2;

                if (!fromAccount.canTransact()) {
                        throw new IllegalStateException("Source account is " + fromAccount.getStatus());
                }

                if (!toAccount.canTransact()) {
                        throw new IllegalStateException("Target account is " + toAccount.getStatus());
                }

                // Validar saldo da conta que envia
                if (fromAccount.getBalance().compareTo(command.amount()) < 0) {
                        throw new InsufficientFundsException("Insufficient funds");
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

                if (command.idempotencyKey() != null) {

                        String jsonBody = String.format("{\"transactionId\": \"%s\"}", transaction.getId());
                        IdempotencyKey key = new IdempotencyKey(
                                        command.idempotencyKey(),
                                        200,
                                        jsonBody);
                        idempotencyRespository.save(key);
                }

                return transaction.getId();
        }
}
