package com.titan.ledger.core.usecase.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record StatementEntryResponse(
        UUID transactionId,
        String operationType, // CREDIT ou DEBIT
        BigDecimal amount,
        BigDecimal balanceAfter, // saldo após a operação
        String description,
        Instant timestamp) {

}
