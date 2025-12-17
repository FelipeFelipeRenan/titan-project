package com.titan.ledger.core.usecase;

import java.util.UUID;

public interface RevertTransactionUseCase {
    void execute(UUID transactionId, String reason);
}
