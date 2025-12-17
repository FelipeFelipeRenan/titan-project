package com.titan.ledger.core.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.titan.ledger.adapter.out.persistence.LedgerRepository;
import com.titan.ledger.adapter.out.persistence.TransactionRepository;
import com.titan.ledger.core.domain.model.LedgerEntry;
import com.titan.ledger.core.domain.model.OperationType;
import com.titan.ledger.core.domain.model.Transaction;
import com.titan.ledger.core.domain.model.TransactionStatus;
import com.titan.ledger.core.usecase.RevertTransactionUseCase;
import com.titan.ledger.core.usecase.TransferFundsUseCase;
import com.titan.ledger.core.usecase.dto.TransferFundsCommand;


@Service
public class RevertService implements RevertTransactionUseCase {

    private final TransactionRepository transactionRepository;
    private final LedgerRepository ledgerRepository;
    private final TransferFundsUseCase transferFundsUseCase;

    public RevertService(TransactionRepository transactionRepository, LedgerRepository ledgerRepository,
            TransferFundsUseCase transferFundsUseCase) {
        this.transactionRepository = transactionRepository;
        this.ledgerRepository = ledgerRepository;
        this.transferFundsUseCase = transferFundsUseCase;
    }

    @Override
    @Transactional
    public void execute(UUID originalTransactionId, String reason) {
        // Buscar transação original
        Transaction originalTx = transactionRepository.findById(originalTransactionId)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found"));

        if (originalTx.getStatus() == TransactionStatus.REVERTED) {
            throw new IllegalStateException("Transaction already reverted");
        }
        if (originalTx.getStatus() != TransactionStatus.COMPLETED) {
            throw new IllegalStateException("Can only revert COMPLETED transactions");
        }

        List<LedgerEntry> entries = ledgerRepository.findByTransaction(originalTx);

        UUID originalSenderId = entries.stream()
                .filter(e -> e.getOperationType() == OperationType.DEBIT)
                .findFirst()
                .orElseThrow()
                .getAccount().getId();

        UUID originalReceiverId = entries.stream()
                .filter(e -> e.getOperationType() == OperationType.CREDIT)
                .findFirst()
                .orElseThrow()
                .getAccount().getId();

        var amount = entries.get(0).getAmount();

        // 4. Executar o Estorno (Criando uma nova transferência inversa)
        TransferFundsCommand revertCommand = new TransferFundsCommand(
                originalReceiverId, // Quem recebeu agora paga
                originalSenderId, // Quem pagou agora recebe
                amount,
                "REVERSAL of " + originalTx.getId() + ": " + reason,
                UUID.randomUUID().toString() // Nova chave de idempotência
        );

        // Isso vai gerar Account Update, Ledger Entry, Outbox Event, Cache Evict... TUDO!
        UUID revertedTxId = transferFundsUseCase.execute(revertCommand);


        // atualizar status do original
        originalTx.setStatus(TransactionStatus.REVERTED);
        originalTx.setRevertedByTransactionId(revertedTxId);
        transactionRepository.save(originalTx);
    }

}
