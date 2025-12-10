package com.titan.ledger.adapter.in.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.titan.ledger.core.usecase.CreateAccountUseCase;
import com.titan.ledger.core.usecase.GetAccountBalanceUseCase;
import com.titan.ledger.core.usecase.dto.AccountResponse;
import com.titan.ledger.core.usecase.dto.CreateAccountCommand;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/accounts")
public class AccountController {
    
    private final CreateAccountUseCase createAccountUseCase;
    private final GetAccountBalanceUseCase getAccountBalanceUseCase;


    public AccountController(CreateAccountUseCase createAccountUseCase,
            GetAccountBalanceUseCase getAccountBalanceUseCase) {
        this.createAccountUseCase = createAccountUseCase;
        this.getAccountBalanceUseCase = getAccountBalanceUseCase;
    }

    @PostMapping
    public ResponseEntity<AccountResponse> createAccount(@RequestBody CreateAccountCommand command){

        AccountResponse response = createAccountUseCase.execute(command);

        if("EXISTING".equals(response.status())){
            return ResponseEntity.ok(response);
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @GetMapping("/{accountId}")
    public ResponseEntity<AccountResponse> getBalance(@PathVariable UUID accountId){
        AccountResponse response = getAccountBalanceUseCase.execute(accountId);
        
        return ResponseEntity.ok(response);
    
    }
}
