package com.titan.ledger.core.usecase;

import java.util.UUID;

import com.titan.ledger.core.usecase.dto.TransferFundsCommand;

public interface TransferFundsUseCase {
    UUID execute(TransferFundsCommand command);
}
