package com.titan.ledger.adapter.in.web;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.titan.ledger.core.usecase.CreateAccountUseCase;
import com.titan.ledger.core.usecase.GetAccountBalanceUseCase;
import com.titan.ledger.core.usecase.dto.AccountResponse;
import com.titan.ledger.core.usecase.dto.CreateAccountCommand;

import java.util.UUID;

import com.titan.ledger.core.service.AccountQueryService;
import com.titan.ledger.core.usecase.DepositUseCase;
import com.titan.ledger.core.usecase.dto.DepositCommand;

@RestController
@RequestMapping("/api/v1/accounts")
public class AccountController {

    private final CreateAccountUseCase createAccountUseCase;
    private final GetAccountBalanceUseCase getAccountBalanceUseCase;
    private final DepositUseCase depositUseCase;
    private final AccountQueryService accountQueryService;

    public AccountController(CreateAccountUseCase createAccountUseCase,
            GetAccountBalanceUseCase getAccountBalanceUseCase, DepositUseCase depositUseCase,
            AccountQueryService accountQueryService) {
        this.createAccountUseCase = createAccountUseCase;
        this.getAccountBalanceUseCase = getAccountBalanceUseCase;
        this.depositUseCase = depositUseCase;
        this.accountQueryService = accountQueryService;
    }

    @GetMapping
    public ResponseEntity<List<AccountResponse>> listAll(){
        return ResponseEntity.ok(accountQueryService.listAll());
    }

    @PostMapping
    public ResponseEntity<AccountResponse> createAccount(@RequestBody CreateAccountCommand command) {

        AccountResponse response = createAccountUseCase.execute(command);

        if ("EXISTING".equals(response.status())) {
            return ResponseEntity.ok(response);
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{accountId}")
    public ResponseEntity<AccountResponse> getBalance(@PathVariable UUID accountId) {
        AccountResponse response = getAccountBalanceUseCase.execute(accountId);

        return ResponseEntity.ok(response);

    }

    @PostMapping("/{accountId}/deposit")
    public ResponseEntity<Void> deposit(@PathVariable UUID accountId, @RequestBody DepositRequest request){
        DepositCommand command = new DepositCommand(
            accountId,
            request.amount(),
            request.description()
        );

        depositUseCase.execute(command);
        return ResponseEntity.ok().build();
    }

    public record DepositRequest(BigDecimal amount, String description){}
}
