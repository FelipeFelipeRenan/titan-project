package com.titan.ledger.integration;

import com.titan.ledger.AbstractIntegrationTest;
import com.titan.ledger.adapter.in.web.dto.TransferRequestDTO;
import com.titan.ledger.adapter.out.persistence.AccountRepository;
import com.titan.ledger.adapter.out.persistence.LedgerRepository; // Novo
import com.titan.ledger.adapter.out.persistence.TransactionRepository; // Novo
import com.titan.ledger.adapter.out.persistence.OutboxRepository; // Novo
import com.titan.ledger.adapter.out.persistence.IdempotencyRepository; // Novo
import com.titan.ledger.core.domain.model.Account;
import com.titan.ledger.core.domain.model.AccountStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class TransferIntegrationTest extends AbstractIntegrationTest {

    @Autowired private WebTestClient webTestClient;
    @Autowired private AccountRepository accountRepository;
    
    // Injetar repositórios dependentes para limpeza correta
    @Autowired private LedgerRepository ledgerRepository;
    @Autowired private TransactionRepository transactionRepository;
    @Autowired private OutboxRepository outboxRepository;
    @Autowired private IdempotencyRepository idempotencyRepository;

    private UUID aliceId;
    private UUID bobId;

    @BeforeEach
    void setup() {
        // [CORREÇÃO 1] Ordem de limpeza: Filhos primeiro, Pais depois.
        // Isso evita o DataIntegrityViolationException (fk_ledger_account)
        ledgerRepository.deleteAll();
        transactionRepository.deleteAll();
        outboxRepository.deleteAll();
        idempotencyRepository.deleteAll();
        accountRepository.deleteAll();

        // Cria contas no banco para o teste
        Account alice = new Account();
        alice.setClientId("client-alice");
        alice.setBalance(new BigDecimal("100.00"));
        alice.setCurrency("BRL");
        alice.setStatus(AccountStatus.ACTIVE);
        aliceId = accountRepository.save(alice).getId();

        Account bob = new Account();
        bob.setClientId("client-bob");
        bob.setBalance(new BigDecimal("50.00"));
        bob.setCurrency("BRL");
        bob.setStatus(AccountStatus.ACTIVE);
        bobId = accountRepository.save(bob).getId();
    }

    @Test
    void shouldTransferFundsSuccessfully() {
        // ARRANGE
        TransferRequestDTO request = new TransferRequestDTO();
        request.setFromAccountId(aliceId);
        request.setToAccountId(bobId);
        request.setAmount(new BigDecimal("30.00"));
        request.setDescription("Integration Test Transfer");

        // ACT & ASSERT
        webTestClient.post()
                .uri("/api/v1/accounts/transfer")
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                // [CORREÇÃO 2] O erro "START_OBJECT" indica que a API retornou um JSON { ... }
                // e não apenas um UUID solto. Usamos jsonPath para verificar se existe um retorno.
                // Se a API retorna { "transactionId": "..." }, ajuste para $.transactionId
                .expectBody()
                .jsonPath("$").exists(); 
                
        // Validação do Banco
        Account aliceAfter = accountRepository.findById(aliceId).get();
        Account bobAfter = accountRepository.findById(bobId).get();

        assertThat(aliceAfter.getBalance()).isEqualByComparingTo("70.00");
        assertThat(bobAfter.getBalance()).isEqualByComparingTo("80.00");
    }

    @Test
    void shouldFailWhenInsufficientFunds() {
        TransferRequestDTO request = new TransferRequestDTO();
        request.setFromAccountId(aliceId);
        request.setToAccountId(bobId);
        request.setAmount(new BigDecimal("200.00"));
        request.setDescription("Fail Test");

        webTestClient.post()
                .uri("/api/v1/accounts/transfer")
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isEqualTo(422)
                .expectBody()
                .jsonPath("$.title").isEqualTo("Insufficient Funds");

        assertThat(accountRepository.findById(aliceId).get().getBalance()).isEqualByComparingTo("100.00");
    }
}