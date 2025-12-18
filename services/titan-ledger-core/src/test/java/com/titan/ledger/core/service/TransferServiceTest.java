package com.titan.ledger.core.service;

import com.titan.ledger.adapter.out.persistence.AccountRepository;
import com.titan.ledger.adapter.out.persistence.IdempotencyRepository;
import com.titan.ledger.adapter.out.persistence.LedgerRepository;
import com.titan.ledger.adapter.out.persistence.OutboxRepository; // Import necessário
import com.titan.ledger.adapter.out.persistence.TransactionRepository;
import com.titan.ledger.core.domain.exception.InsufficientFundsException;
import com.titan.ledger.core.domain.model.Account;
import com.titan.ledger.core.domain.model.AccountStatus;
import com.titan.ledger.core.domain.model.LedgerEntry;
import com.titan.ledger.core.domain.model.Transaction;
import com.titan.ledger.core.usecase.dto.TransferFundsCommand;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransferServiceTest {

    @Mock private AccountRepository accountRepository;
    @Mock private TransactionRepository transactionRepository;
    @Mock private LedgerRepository ledgerRepository;
    @Mock private IdempotencyRepository idempotencyRepository;
    
    // --- NOVOS MOCKS NECESSÁRIOS PARA O SPRING BOOT 4 / VERSÃO ATUAL DO CÓDIGO ---
    @Mock private OutboxRepository outboxRepository;
    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;
    
    @Spy private MeterRegistry meterRegistry = new SimpleMeterRegistry();

    @InjectMocks private TransferService transferService;

    @BeforeEach
    void setup() {
        // Necessário para evitar NullPointer quando o serviço acessa o Redis
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    @DisplayName("Should transfer funds successfully between two active accounts")
    void shouldTransferFunds() {
        UUID idAlice = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID idBob = UUID.fromString("22222222-2222-2222-2222-222222222222");

        Account alice = new Account("alice", "BRL");
        alice.setId(idAlice);
        alice.setBalance(new BigDecimal("100.00"));

        Account bob = new Account("bob", "BRL");
        bob.setId(idBob);
        bob.setBalance(new BigDecimal("0.00"));

        when(accountRepository.findByIdForUpdate(idAlice)).thenReturn(Optional.of(alice));
        when(accountRepository.findByIdForUpdate(idBob)).thenReturn(Optional.of(bob));

        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction t = invocation.getArgument(0);
            t.setId(UUID.randomUUID());
            return t;
        });

        TransferFundsCommand command = new TransferFundsCommand(
            idAlice, idBob, new BigDecimal("50.00"), "Test", null
        );

        UUID txId = transferService.execute(command);

        assertThat(txId).isNotNull();
        assertThat(alice.getBalance()).isEqualByComparingTo("50.00");
        assertThat(bob.getBalance()).isEqualByComparingTo("50.00");

        verify(outboxRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("Should fail when source account has insufficient funds")
    void shouldFailInsufficientFunds() {
        UUID idAlice = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID idBob = UUID.fromString("22222222-2222-2222-2222-222222222222");

        Account alice = new Account("alice", "BRL");
        alice.setId(idAlice);
        alice.setBalance(new BigDecimal("10.00"));

        Account bob = new Account("bob", "BRL");
        bob.setId(idBob);

        when(accountRepository.findByIdForUpdate(idAlice)).thenReturn(Optional.of(alice));
        when(accountRepository.findByIdForUpdate(idBob)).thenReturn(Optional.of(bob));

        TransferFundsCommand command = new TransferFundsCommand(
            idAlice, idBob, new BigDecimal("50.00"), "Test", null
        );

        assertThatThrownBy(() -> transferService.execute(command))
                .isInstanceOf(InsufficientFundsException.class);
    }
    
    @Test
    @DisplayName("Should fail when source account is FROZEN")
    void shouldFailFrozenAccount() {
        UUID idAlice = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID idBob = UUID.fromString("22222222-2222-2222-2222-222222222222");

        Account alice = new Account("alice", "BRL");
        alice.setId(idAlice);
        alice.setBalance(new BigDecimal("100.00"));
        alice.setStatus(AccountStatus.FROZEN);

        Account bob = new Account("bob", "BRL");
        bob.setId(idBob);

        when(accountRepository.findByIdForUpdate(idAlice)).thenReturn(Optional.of(alice));
        when(accountRepository.findByIdForUpdate(idBob)).thenReturn(Optional.of(bob));

        TransferFundsCommand command = new TransferFundsCommand(
            idAlice, idBob, new BigDecimal("50.00"), "Test", null
        );

        assertThatThrownBy(() -> transferService.execute(command))
                .isInstanceOf(IllegalStateException.class);
    }
}