package com.titan.ledger.adapter.out.persistence;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.titan.ledger.core.domain.model.Account;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class AccountRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @Autowired
    private AccountRepository accountRepository;

    @Test
    @DisplayName("Should save an Account and find it by ClientID")
    void shouldSaveAndFindAccount() {
        String clientId = "user-123";
        String currency = "BRL";
        // Lembre-se de ter adicionado o construtor na entidade Account como mencionado anteriormente
        Account newAccount = new Account(clientId, currency);

        Account savedAccount = accountRepository.save(newAccount);

        assertThat(savedAccount.getId()).isNotNull();
        assertThat(savedAccount.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);

        Optional<Account> foundAccount = accountRepository.findByClientIdAndCurrency(clientId, currency);
        
        assertThat(foundAccount).isPresent();
        assertThat(foundAccount.get().getClientId()).isEqualTo(clientId);
    }
}