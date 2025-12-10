package com.titan.ledger.core.usecase.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record DepositCommand(
        UUID accountId,
        BigDecimal amount,
        String description) {
    public DepositCommand {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Deposit amount must be positive ");
        }
    }
}
