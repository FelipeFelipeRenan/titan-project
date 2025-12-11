package com.titan.ledger.adapter.out.persistence;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.titan.ledger.core.domain.model.Transaction;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

}
