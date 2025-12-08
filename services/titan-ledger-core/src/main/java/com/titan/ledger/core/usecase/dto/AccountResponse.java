package com.titan.ledger.core.usecase.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record AccountResponse(
    UUID id,
    String clientId,
    String currency,
    BigDecimal balance,
    String status
) {
    
}
