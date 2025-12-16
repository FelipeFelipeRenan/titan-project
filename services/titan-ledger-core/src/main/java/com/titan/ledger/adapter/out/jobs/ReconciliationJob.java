package com.titan.ledger.adapter.out.jobs;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.titan.ledger.adapter.out.persistence.AccountRepository;
import com.titan.ledger.adapter.out.persistence.LedgerRepository;
import com.titan.ledger.core.domain.model.Account;

@Component
public class ReconciliationJob {
    private static final Logger logger = LoggerFactory.getLogger(ReconciliationJob.class);

    private final LedgerRepository ledgerRepository;
    private final AccountRepository accountRepository;

    public ReconciliationJob(LedgerRepository ledgerRepository, AccountRepository accountRepository) {
        this.ledgerRepository = ledgerRepository;
        this.accountRepository = accountRepository;
    }

    @Scheduled(fixedDelay = 60000)
    @Transactional(readOnly = true)
    public void runReconciliation() {
        logger.info("-------Starting Account Reconciliation Job--------");

        List<LedgerRepository.BalanceSummary> ledgerBalances = ledgerRepository.getBalancesFromLedger();

        Map<UUID, BigDecimal> calculatedMap = ledgerBalances.stream()
                .collect(Collectors.toMap(
                        LedgerRepository.BalanceSummary::getAccountId,
                        LedgerRepository.BalanceSummary::getCalculatedBalance));

        List<Account> accounts = accountRepository.findAll();

        int discrepancies = 0;

        for (Account account : accounts) {
            BigDecimal currentBalance = account.getBalance();
            BigDecimal realBalance = calculatedMap.getOrDefault(account.getId(), BigDecimal.ZERO);

            if (currentBalance.compareTo(realBalance) != 0) {
                discrepancies++;
                handleDiscrepancy(account.getId(), currentBalance, realBalance);

            }

        }

        if (discrepancies == 0) {
            logger.info("✅ Reconciliation finished. All accounts are balanced.");
        } else {
            logger.error("🚨 Reconciliation finished with {} discrepancies!", discrepancies);
        }
    }

    private void handleDiscrepancy(UUID accountId, BigDecimal current, BigDecimal real) {
        // AQUI É O PÂNICO
        // Num banco real, isso congelaria a conta e abriria um ticket no
        // Jira/ServiceNow
        logger.error("❌ DISCREPANCY DETECTED! Account: {} | Account Table: {} | Ledger Sum: {}",
                accountId, current, real);

        // Sugestão futura: Salvar na tabela 'audit_logs'
    }
}
