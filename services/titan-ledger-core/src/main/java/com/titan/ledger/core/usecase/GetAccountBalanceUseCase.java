package com.titan.ledger.core.usecase;

import java.util.UUID;

import com.titan.ledger.core.usecase.dto.AccountResponse;

public interface GetAccountBalanceUseCase {
    
    AccountResponse execute(UUID accountId);
}
