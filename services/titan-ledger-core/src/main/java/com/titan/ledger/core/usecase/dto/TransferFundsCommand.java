package com.titan.ledger.core.usecase.dto;

import java.math.BigDecimal;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TransferFundsCommand(
@JsonProperty("fromAccountId") UUID fromAccountId,
    @JsonProperty("toAccountId") UUID toAccountId,
    @JsonProperty("amount") BigDecimal amount,
    @JsonProperty("description") String description,
    @JsonProperty("idempotencyKey") String idempotencyKey
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
