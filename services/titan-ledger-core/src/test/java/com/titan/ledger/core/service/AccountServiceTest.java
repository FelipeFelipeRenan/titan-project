package com.titan.ledger.core.service;

import com.titan.ledger.adapter.out.persistence.AccountRepository;
import com.titan.ledger.core.domain.model.Account;
import com.titan.ledger.core.usecase.dto.AccountResponse;
import com.titan.ledger.core.usecase.dto.CreateAccountCommand;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class) // Habilita o Mockito no JUnit 5
class AccountServiceTest {

    // @Mock cria uma "casca" falsa do repositório. Ele não conecta em banco nenhum.
    @Mock
    private AccountRepository accountRepository;

    // @InjectMocks pega o Mock acima e injeta dentro do Service real.
    @InjectMocks
    private AccountService accountService;

    @Test
    @DisplayName("Should create a new account when it does not exist")
    void shouldCreateNewAccount() {
        // --- ARRANGE (Preparar a mentira) ---
        CreateAccountCommand command = new CreateAccountCommand("client-1", "BRL");

        // 1. Quando buscar, diga que NÃO achou nada (Optional.empty)
        when(accountRepository.findByClientIdAndCurrency("client-1", "BRL"))
                .thenReturn(Optional.empty());

        // 2. Quando pedir pra salvar, retorne uma conta com ID gerado
        Account savedAccountMock = new Account("client-1", "BRL");
        savedAccountMock.setId(UUID.randomUUID());
        
        when(accountRepository.save(any(Account.class)))
                .thenReturn(savedAccountMock);

        // --- ACT (Executar) ---
        AccountResponse response = accountService.execute(command);

        // --- ASSERT (Validar) ---
        assertThat(response.id()).isNotNull();
        assertThat(response.status()).isEqualTo("CREATED");
        assertThat(response.balance()).isEqualByComparingTo(BigDecimal.ZERO);

        // Verifica se o método save() foi chamado exatamente 1 vez
        verify(accountRepository, times(1)).save(any(Account.class));
    }

    @Test
    @DisplayName("Should return existing account (Idempotency) when it already exists")
    void shouldReturnExistingAccount() {
        // --- ARRANGE ---
        CreateAccountCommand command = new CreateAccountCommand("client-1", "BRL");
        
        // Simula uma conta que já estava no banco
        Account existingAccount = new Account("client-1", "BRL");
        existingAccount.setId(UUID.randomUUID());
        existingAccount.setBalance(new BigDecimal("100.00")); // Já tem dinheiro

        // Quando buscar, diga que ACHOU (Optional.of)
        when(accountRepository.findByClientIdAndCurrency("client-1", "BRL"))
                .thenReturn(Optional.of(existingAccount));

        // --- ACT ---
        AccountResponse response = accountService.execute(command);

        // --- ASSERT ---
        assertThat(response.id()).isEqualTo(existingAccount.getId());
        assertThat(response.status()).isEqualTo("EXISTING"); // Status diferente
        assertThat(response.balance()).isEqualByComparingTo(new BigDecimal("100.00"));

        // O PULO DO GATO: Garante que o método save() NUNCA foi chamado
        // Isso prova que não duplicamos dados nem gastamos I/O do banco à toa.
        verify(accountRepository, never()).save(any(Account.class));
    }
}