package com.titan.ledger.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.titan.ledger.core.domain.model.IdempotencyKey;

@Repository
public interface IdempotencyRepository extends JpaRepository<IdempotencyKey, String>{
    
}
