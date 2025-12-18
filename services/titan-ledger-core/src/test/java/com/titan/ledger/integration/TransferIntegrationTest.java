package com.titan.ledger.integration;

import com.titan.ledger.AbstractIntegrationTest;
import com.titan.ledger.adapter.in.web.dto.TransferRequestDTO;
import com.titan.ledger.adapter.out.persistence.AccountRepository;
import com.titan.ledger.core.domain.model.Account;
import com.titan.ledger.core.domain.model.AccountStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient; // <--- O Import Novo

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class TransferIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private WebTestClient webTestClient; // <--- Injeção do cliente moderno

    @Autowired
    private AccountRepository accountRepository;

    private UUID aliceId;
    private UUID bobId;

    @BeforeEach
    void setup() {
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
                // --- CORREÇÃO AQUI ---
                .expectBody(UUID.class) // Converte o JSON para UUID
                .value(uuid -> assertThat(uuid).isNotNull()); // Valida que chegou algo
                
        // Validação do Banco continua igual...
        Account aliceAfter = accountRepository.findById(aliceId).get();
        Account bobAfter = accountRepository.findById(bobId).get();

        assertThat(aliceAfter.getBalance()).isEqualByComparingTo("70.00");
        assertThat(bobAfter.getBalance()).isEqualByComparingTo("80.00");
    }
    @Test
    void shouldFailWhenInsufficientFunds() {
        // Tenta transferir 200 (tem 100)
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
                .expectStatus().isEqualTo(422) // Valida 422 Unprocessable Entity
                .expectBody()
                .jsonPath("$.title").isEqualTo("Insufficient Funds"); // Valida o JSON de erro

        // Garante que o saldo não mudou
        assertThat(accountRepository.findById(aliceId).get().getBalance()).isEqualByComparingTo("100.00");
    }
}