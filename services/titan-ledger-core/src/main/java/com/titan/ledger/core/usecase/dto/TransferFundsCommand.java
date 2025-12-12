package com.titan.ledger.core.usecase.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record TransferFundsCommand(
    UUID fromAccountId,
    UUID toAccountId,
    BigDecimal amount,
    String description,
    String idempotencyKey
) {
    public TransferFundsCommand{
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Transfer amount must be positive");
        }
        if (fromAccountId.equals(toAccountId)) {
            throw new IllegalArgumentException("Cannot transfer to the same account");
        }
    }
}
