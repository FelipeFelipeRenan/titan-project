package com.titan.ledger.adapter.out.persistence;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.titan.ledger.core.domain.model.LedgerEntry;

public interface LedgerRepository extends JpaRepository<LedgerEntry, UUID> {

}
