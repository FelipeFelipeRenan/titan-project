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

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;

import com.titan.ledger.core.service.AccountQueryService;
import com.titan.ledger.core.usecase.DepositUseCase;
import com.titan.ledger.core.usecase.TransferFundsUseCase;
import com.titan.ledger.core.usecase.dto.DepositCommand;
import com.titan.ledger.core.usecase.dto.StatementEntryResponse;
import com.titan.ledger.core.usecase.dto.TransferFundsCommand;

@RestController
@RequestMapping("/api/v1/accounts")
public class AccountController {

    private final CreateAccountUseCase createAccountUseCase;
    private final GetAccountBalanceUseCase getAccountBalanceUseCase;
    private final DepositUseCase depositUseCase;
    private final AccountQueryService accountQueryService;
    private final TransferFundsUseCase transferFundsUseCase;

    public AccountController(CreateAccountUseCase createAccountUseCase,
            GetAccountBalanceUseCase getAccountBalanceUseCase, DepositUseCase depositUseCase,
            AccountQueryService accountQueryService, TransferFundsUseCase transferFundsUseCase) {
        this.createAccountUseCase = createAccountUseCase;
        this.getAccountBalanceUseCase = getAccountBalanceUseCase;
        this.depositUseCase = depositUseCase;
        this.accountQueryService = accountQueryService;
        this.transferFundsUseCase = transferFundsUseCase;
    }

    @GetMapping
    public ResponseEntity<List<AccountResponse>> listAll() {
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
    public ResponseEntity<Void> deposit(@PathVariable UUID accountId, @RequestBody DepositRequest request) {
        DepositCommand command = new DepositCommand(
                accountId,
                request.amount(),
                request.description());

        depositUseCase.execute(command);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/transfer")
    public ResponseEntity<TransactionIdResponse> transfer(
        @RequestBody TransferRequest request,
        @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey 
    ) {

        TransferFundsCommand command = new TransferFundsCommand(request.fromAccountId(), request.toAccountId(),
                request.amount(), request.description(), idempotencyKey);

        UUID transactionId = transferFundsUseCase.execute(command);

        return ResponseEntity.ok(new TransactionIdResponse(transactionId));
    }

    @GetMapping("/{accountId}/statement")
    public ResponseEntity<Page<StatementEntryResponse>> getStatement(
            @PathVariable UUID accountId,
            // @PageableDefault define o padrao se o usuario nao mandar nada
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<StatementEntryResponse> statement = accountQueryService.getStatement(accountId, pageable);
        return ResponseEntity.ok(statement);
    }

    public record DepositRequest(BigDecimal amount, String description) {
    }

    public record TransferRequest(UUID fromAccountId, UUID toAccountId, BigDecimal amount, String description) {
    }

    public record TransactionIdResponse(UUID transactionId) {
    }
}
