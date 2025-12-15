package com.titan.ledger.core.service;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.redis.core.StringRedisTemplate;
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
    private final IdempotencyRepository idempotencyRepository; 
    private final StringRedisTemplate redisTemplate;

    public TransferService(AccountRepository accountRepository, 
                           TransactionRepository transactionRepository,
                           LedgerRepository ledgerRepository, 
                           IdempotencyRepository idempotencyRepository,
                           StringRedisTemplate redisTemplate) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.ledgerRepository = ledgerRepository;
        this.idempotencyRepository = idempotencyRepository;
        this.redisTemplate = redisTemplate;
    }

    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "accounts", key = "#command.fromAccountId()"),
        @CacheEvict(value = "accounts", key = "#command.toAccountId()"),
        @CacheEvict(value = "statements", key = "#command.fromAccountId() + '::0'"),
        @CacheEvict(value = "statements", key = "#command.toAccountId() + '::0'")
    })
    public UUID execute(TransferFundsCommand command) {
        // 1. IDEMPOTENCY CHECK (Redis Primeiro, depois Banco)
        if (command.idempotencyKey() != null) {
            
            // Check Redis
            String cachedTxId = redisTemplate.opsForValue().get("idem::" + command.idempotencyKey());
            if (cachedTxId != null) {
                return UUID.fromString(cachedTxId);
            }

            // Check Postgres (Fallback)
            return idempotencyRepository.findById(command.idempotencyKey())
                .map(existing -> {
                    String uuid = extractUuidFromJson(existing.getResponseBody());
                    // Popula o Redis para a próxima vez ser rápida
                    cacheIdempotencyKey(command.idempotencyKey(), uuid);
                    return UUID.fromString(uuid);
                })
                .orElseGet(() -> processNewTransfer(command));
        }

        return processNewTransfer(command);
    }

    // Lógica principal de transferência
    private UUID processNewTransfer(TransferFundsCommand command) {
        // DEADLOCK PREVENTION
        UUID firstLockId = command.fromAccountId().compareTo(command.toAccountId()) < 0
                ? command.fromAccountId()
                : command.toAccountId();

        UUID secondLockId = command.fromAccountId().compareTo(command.toAccountId()) < 0
                ? command.toAccountId()
                : command.fromAccountId();

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

        if (fromAccount.getBalance().compareTo(command.amount()) < 0) {
            throw new InsufficientFundsException("Insufficient funds");
        }

        Transaction transaction = new Transaction(
                UUID.randomUUID().toString(),
                command.description());
        transaction.setStatus(TransactionStatus.COMPLETED);
        transactionRepository.save(transaction);

        BigDecimal newSourceBalance = fromAccount.getBalance().subtract(command.amount());
        BigDecimal newTargetBalance = toAccount.getBalance().add(command.amount());

        fromAccount.setBalance(newSourceBalance);
        toAccount.setBalance(newTargetBalance);

        accountRepository.save(fromAccount);
        accountRepository.save(toAccount);

        LedgerEntry debitEntry = new LedgerEntry(
                transaction, fromAccount, OperationType.DEBIT, command.amount(), newSourceBalance);

        LedgerEntry creditEntry = new LedgerEntry(
                transaction, toAccount, OperationType.CREDIT, command.amount(), newTargetBalance);

        ledgerRepository.save(debitEntry);
        ledgerRepository.save(creditEntry);

        // 2. SALVAR IDEMPOTÊNCIA (Se houver chave)
        if (command.idempotencyKey() != null) {
            // Formata o JSON manualmente: {"transactionId": "UUID"}
            String jsonBody = String.format("{\"transactionId\": \"%s\"}", transaction.getId());

            // Salva no Postgres (AQUI ESTAVA O ERRO DE PLACEHOLDER '...')
            IdempotencyKey key = new IdempotencyKey(
                command.idempotencyKey(),
                200,
                jsonBody
            );
            idempotencyRepository.save(key);

            // Salva no Redis
            cacheIdempotencyKey(command.idempotencyKey(), transaction.getId().toString());
        }

        return transaction.getId();
    }

    // Método auxiliar para salvar no Redis
    private void cacheIdempotencyKey(String key, String txId) {
        redisTemplate.opsForValue().set(
                "idem::" + key,
                txId,
                24, TimeUnit.HOURS);
    }

    // Método auxiliar para extrair UUID do JSON (AQUI ESTAVA O ERRO DE MÉTODO FALTANDO)
    private String extractUuidFromJson(String json) {
        // Remove {"transactionId": " e "}
        // É um parse manual simples para evitar importar Jackson aqui só pra isso
        return json.replace("{\"transactionId\": \"", "").replace("\"}", "");
    }
}