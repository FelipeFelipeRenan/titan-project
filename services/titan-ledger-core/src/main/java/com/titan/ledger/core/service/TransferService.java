package com.titan.ledger.core.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.titan.ledger.adapter.out.persistence.*;
import com.titan.ledger.core.domain.exception.AccountNotFoundException;
import com.titan.ledger.core.domain.exception.InsufficientFundsException;
import com.titan.ledger.core.domain.model.*;
import com.titan.ledger.core.usecase.TransferFundsUseCase;
import com.titan.ledger.core.usecase.dto.TransferFundsCommand;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class TransferService implements TransferFundsUseCase {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final LedgerRepository ledgerRepository;
    private final IdempotencyRepository idempotencyRepository;
    private final OutboxRepository outboxRepository; // Repositório da Outbox
    private final StringRedisTemplate redisTemplate;
    
    // Mapper exclusivo para gerar JSON limpo na Outbox (sem tipos Java)
    private final ObjectMapper eventMapper; 

    public TransferService(AccountRepository accountRepository,
                           TransactionRepository transactionRepository,
                           LedgerRepository ledgerRepository,
                           IdempotencyRepository idempotencyRepository,
                           OutboxRepository outboxRepository,
                           StringRedisTemplate redisTemplate) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.ledgerRepository = ledgerRepository;
        this.idempotencyRepository = idempotencyRepository;
        this.outboxRepository = outboxRepository;
        this.redisTemplate = redisTemplate;

        // Configuração Manual do Mapper para garantir JSON interoperável (Limpo)
        this.eventMapper = new ObjectMapper();
        this.eventMapper.registerModule(new JavaTimeModule()); // Suporte a Instant
        this.eventMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS); // Datas como ISO-8601 String
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
        // 1. IDEMPOTENCY CHECK (Redis Primeiro)
        if (command.idempotencyKey() != null) {
            String cachedTxId = redisTemplate.opsForValue().get("idem::" + command.idempotencyKey());
            if (cachedTxId != null) {
                return UUID.fromString(cachedTxId);
            }

            // Check Postgres (Fallback)
            return idempotencyRepository.findById(command.idempotencyKey())
                .map(existing -> {
                    String uuid = extractUuidFromJson(existing.getResponseBody());
                    cacheIdempotencyKey(command.idempotencyKey(), uuid);
                    return UUID.fromString(uuid);
                })
                .orElseGet(() -> processNewTransfer(command));
        }

        return processNewTransfer(command);
    }

    private UUID processNewTransfer(TransferFundsCommand command) {
        // --- LOGICA DE NEGOCIO (Lock Pessimista) ---
        UUID firstLockId = command.fromAccountId().compareTo(command.toAccountId()) < 0
                ? command.fromAccountId() : command.toAccountId();
        UUID secondLockId = command.fromAccountId().compareTo(command.toAccountId()) < 0
                ? command.toAccountId() : command.fromAccountId();

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

        // --- TRANSAÇÃO E LEDGER ---
        Transaction transaction = new Transaction(UUID.randomUUID().toString(), command.description());
        transaction.setStatus(TransactionStatus.COMPLETED);
        transactionRepository.save(transaction);

        BigDecimal newSourceBalance = fromAccount.getBalance().subtract(command.amount());
        BigDecimal newTargetBalance = toAccount.getBalance().add(command.amount());

        fromAccount.setBalance(newSourceBalance);
        toAccount.setBalance(newTargetBalance);

        accountRepository.save(fromAccount);
        accountRepository.save(toAccount);

        ledgerRepository.save(new LedgerEntry(transaction, fromAccount, OperationType.DEBIT, command.amount(), newSourceBalance));
        ledgerRepository.save(new LedgerEntry(transaction, toAccount, OperationType.CREDIT, command.amount(), newTargetBalance));

        // --- OUTBOX PATTERN (Salvar Evento Limpo) ---
        try {
            TransferCreatedEvent eventPayload = new TransferCreatedEvent(
                transaction.getId().toString(),
                fromAccount.getId().toString(),
                toAccount.getId().toString(),
                command.amount(),
                transaction.getCreatedAt()
            );

            // Usa o eventMapper local para gerar JSON limpo: {"amount": 100.00}
            String jsonPayload = eventMapper.writeValueAsString(eventPayload);

            OutboxEvent outboxEvent = new OutboxEvent(
                "ACCOUNT",
                fromAccount.getId().toString(),
                "TRANSFER_CREATED",
                jsonPayload
            );
            
            outboxRepository.save(outboxEvent);

        } catch (Exception e) {
            // Se falhar a serialização, rollback em tudo para garantir consistência
            throw new RuntimeException("Failed to create outbox event", e);
        }

        // --- SAVE IDEMPOTENCY ---
        if (command.idempotencyKey() != null) {
            String jsonBody = String.format("{\"transactionId\": \"%s\"}", transaction.getId());
            idempotencyRepository.save(new IdempotencyKey(command.idempotencyKey(), 200, jsonBody));
            cacheIdempotencyKey(command.idempotencyKey(), transaction.getId().toString());
        }

        return transaction.getId();
    }

    private void cacheIdempotencyKey(String key, String txId) {
        redisTemplate.opsForValue().set("idem::" + key, txId, 24, TimeUnit.HOURS);
    }

    private String extractUuidFromJson(String json) {
        return json.replace("{\"transactionId\": \"", "").replace("\"}", "");
    }

    // Record interno para representar o Payload do evento
    record TransferCreatedEvent(
        String transactionId,
        String fromAccountId,
        String toAccountId,
        BigDecimal amount,
        Instant timestamp
    ) {}
}