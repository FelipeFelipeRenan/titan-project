package com.titan.ledger.core.usecase;

import com.titan.ledger.core.usecase.dto.DepositCommand;

public interface  DepositUseCase {
    void execute(DepositCommand command);
}
