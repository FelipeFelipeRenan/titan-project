package com.titan.ledger.adapter.out.persistence;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.titan.ledger.core.domain.model.LedgerEntry;
import com.titan.ledger.core.domain.model.Transaction;

@Repository
public interface LedgerRepository extends JpaRepository<LedgerEntry, UUID> {

    // 1. "JOIN FETCH l.transaction" -> Carrega a descrição junto, evitando N+1
    // queries.
    // 2. "WHERE l.account.id" -> Filtra pelo usuário.
    // 3. "Pageable" -> O Spring adiciona automaticamente LIMIT e OFFSET no SQL.
    @EntityGraph(attributePaths = "transaction")
    Page<LedgerEntry> findByAccount_Id(UUID accountId, Pageable pageable);

    interface BalanceSummary {
        UUID getAccountId();

        BigDecimal getCalculatedBalance();
    }

    @Query(value = """
            SELECT
                l.account_id as accountId,
                SUM(CASE
                    WHEN l.operation_type = 'CREDIT' THEN l.amount
                    WHEN l.operation_type = 'DEBIT' THEN -l.amount
                    ELSE 0
                END) as calculatedBalance
            FROM ledger_entries l
            GROUP BY l.account_id
            """, nativeQuery = true)
    List<BalanceSummary> getBalancesFromLedger();


    @Query(value = """
            SELECT
                l.account_id as accountId,
                SUM(CASE
                    WHEN l.operation_type = 'CREDIT' THEN l.amount
                    WHEN l.operation_type = 'DEBIT' THEN -l.amount
                    ELSE 0
                END) as calculatedBalance
            FROM ledger_entries l
            WHERE l.account_id IN :accountIds
            GROUP BY l.account_id
            """, nativeQuery = true)
    List<BalanceSummary> getBalancesForAccounts(@Param("accountIds") List<UUID> accountIds);
    List<LedgerEntry> findByTransaction(Transaction transaction);

}
