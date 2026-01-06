package com.titan.ledger.adapter.out.jobs;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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

    private static final int BATCH_SIZE = 1000;

    public ReconciliationJob(LedgerRepository ledgerRepository, AccountRepository accountRepository) {
        this.ledgerRepository = ledgerRepository;
        this.accountRepository = accountRepository;
    }

    @Scheduled(fixedDelay = 60000)
    public void runReconciliation() {
        logger.info("-------Starting Account Reconciliation Job--------");

        int page = 0;
        int totalDiscrepancies = 0;
        Page<Account> accountPage;

        do {
            accountPage = fetchPage(page);
            List<Account> accounts = accountPage.getContent();

            if (accounts.isEmpty())
                break;

            List<UUID> accountsIds = accounts.stream().map(Account::getId).toList();

            List<LedgerRepository.BalanceSummary> ledgerBalances = ledgerRepository.getBalancesForAccounts(accountsIds);

            Map<UUID, BigDecimal> calculatedMap = ledgerBalances.stream()
                    .collect(Collectors.toMap(
                            LedgerRepository.BalanceSummary::getAccountId,
                            LedgerRepository.BalanceSummary::getCalculatedBalance));

            for (Account account : accounts) {
                BigDecimal currentBalance = account.getBalance();

                BigDecimal realBalance = calculatedMap.getOrDefault(account.getId(), BigDecimal.ZERO);

                if (currentBalance.compareTo(realBalance) != 0) {
                    totalDiscrepancies++;
                    handleDiscrepancy(account.getId(), currentBalance, realBalance);
                }
            }

            page++;

            logger.debug("Processed page {}/{}", page, accountPage.getTotalPages());

        } while (accountPage.hasNext());

        if (totalDiscrepancies == 0) {
            logger.info("✅ Reconciliation finished. Checked {} accounts. System Balanced.",
                    accountPage.getTotalElements());
        } else {
            logger.error("🚨 Reconciliation finished with {} discrepancies!", totalDiscrepancies);
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

    @Transactional(readOnly = true)
    protected Page<Account> fetchPage(int page) {
        return accountRepository.findAll(PageRequest.of(page, BATCH_SIZE));
    }
}
