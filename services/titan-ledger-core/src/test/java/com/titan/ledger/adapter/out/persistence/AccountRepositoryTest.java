package com.titan.ledger.adapter.out.persistence;

import com.titan.ledger.AbstractIntegrationTest;
import com.titan.ledger.core.domain.model.Account;
import com.titan.ledger.core.domain.model.AccountStatus; // [CORREÇÃO] Import necessário
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class AccountRepositoryTest extends AbstractIntegrationTest {

    @Autowired
    private AccountRepository accountRepository;
    
    @BeforeEach
    void setup() {
        accountRepository.deleteAll();
    }

    @Test
    @DisplayName("Should save an Account and find it by ClientID")
    void shouldSaveAndFindAccount() {
        String clientId = "user-123";
        String currency = "BRL";
        
        Account newAccount = new Account();
        newAccount.setClientId(clientId);
        newAccount.setCurrency(currency);
        newAccount.setBalance(BigDecimal.ZERO);
        // [CORREÇÃO] É obrigatório definir o status, senão o banco recusa (DataIntegrityViolation)
        newAccount.setStatus(AccountStatus.ACTIVE); 

        Account savedAccount = accountRepository.save(newAccount);

        assertThat(savedAccount.getId()).isNotNull();
        assertThat(savedAccount.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);

        Optional<Account> foundAccount = accountRepository.findByClientIdAndCurrency(clientId, currency);
        
        assertThat(foundAccount).isPresent();
        assertThat(foundAccount.get().getClientId()).isEqualTo(clientId);
    }
}