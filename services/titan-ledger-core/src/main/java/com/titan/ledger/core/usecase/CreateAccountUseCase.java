package com.titan.ledger.core.usecase;

import com.titan.ledger.core.usecase.dto.AccountResponse;
import com.titan.ledger.core.usecase.dto.CreateAccountCommand;

public interface CreateAccountUseCase{
    AccountResponse execute(CreateAccountCommand command);
}