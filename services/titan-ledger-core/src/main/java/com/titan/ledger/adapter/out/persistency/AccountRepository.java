package com.titan.ledger.adapter.out.persistency;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.titan.ledger.core.domain.model.Account;


@Repository
public interface AccountRepository extends JpaRepository<Account, UUID>{
        
    Optional<Account> findByClientIdAndCurrency(String clientId, String currency);
}
