package com.titan.ledger.adapter.in.web;

import com.titan.ledger.adapter.in.web.dto.CreateAccountRequestDTO;
import com.titan.ledger.adapter.in.web.dto.DepositRequestDTO;
import com.titan.ledger.adapter.in.web.dto.TransferRequestDTO;
import com.titan.ledger.core.service.AccountQueryService;
import com.titan.ledger.core.usecase.CreateAccountUseCase;
import com.titan.ledger.core.usecase.DepositUseCase;
import com.titan.ledger.core.usecase.GetAccountBalanceUseCase;
import com.titan.ledger.core.usecase.TransferFundsUseCase;
import com.titan.ledger.core.usecase.dto.*; // Seus DTOs de UseCase (AccountResponse, etc)

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/accounts")
@Tag(name = "Accounts", description = "Gestão de Contas, Extratos e Movimentações")
public class AccountController {

    private final CreateAccountUseCase createAccountUseCase;
    private final GetAccountBalanceUseCase getAccountBalanceUseCase;
    private final DepositUseCase depositUseCase;
    private final AccountQueryService accountQueryService;
    private final TransferFundsUseCase transferFundsUseCase;

    public AccountController(CreateAccountUseCase createAccountUseCase,
                             GetAccountBalanceUseCase getAccountBalanceUseCase,
                             DepositUseCase depositUseCase,
                             AccountQueryService accountQueryService,
                             TransferFundsUseCase transferFundsUseCase) {
        this.createAccountUseCase = createAccountUseCase;
        this.getAccountBalanceUseCase = getAccountBalanceUseCase;
        this.depositUseCase = depositUseCase;
        this.accountQueryService = accountQueryService;
        this.transferFundsUseCase = transferFundsUseCase;
    }

    // --- LISTAR TODAS ---
    @GetMapping
    @Operation(summary = "Listar todas as contas", description = "Retorna uma lista completa de todas as contas registradas no Ledger (Use com cuidado em produção).")
    public ResponseEntity<List<AccountResponse>> listAll() {
        return ResponseEntity.ok(accountQueryService.listAll());
    }

    // --- CRIAR CONTA ---
    @PostMapping
    @Operation(summary = "Criar nova conta", description = "Cria uma conta para um Client ID específico. Se já existir, retorna a existente.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Conta criada com sucesso"),
        @ApiResponse(responseCode = "200", description = "Conta já existia, retornando dados atuais")
    })
    public ResponseEntity<AccountResponse> createAccount(@RequestBody CreateAccountRequestDTO request) {
        // Converte DTO Web para DTO de UseCase
        CreateAccountCommand command = new CreateAccountCommand(request.getClientId(), "BRL");
        
        AccountResponse response = createAccountUseCase.execute(command);

        if ("EXISTING".equals(response.status())) {
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // --- SALDO ---
    @GetMapping("/{accountId}")
    @Operation(summary = "Consultar Saldo", description = "Obtém o saldo atual e status de uma conta específica.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Sucesso"),
        @ApiResponse(responseCode = "404", description = "Conta não encontrada", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<AccountResponse> getBalance(@PathVariable UUID accountId) {
        AccountResponse response = getAccountBalanceUseCase.execute(accountId);
        return ResponseEntity.ok(response);
    }

    // --- DEPÓSITO ---
    @PostMapping("/{accountId}/deposit")
    @Operation(summary = "Realizar Depósito (Cash-in)", description = "Adiciona fundos a uma conta. Gera uma entrada no Ledger e um evento de notificação.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Depósito realizado"),
        @ApiResponse(responseCode = "404", description = "Conta não encontrada")
    })
    public ResponseEntity<Void> deposit(@PathVariable UUID accountId, @RequestBody DepositRequestDTO request) {
        DepositCommand command = new DepositCommand(
                accountId,
                request.getAmount(),
                request.getDescription());

        depositUseCase.execute(command);
        return ResponseEntity.ok().build();
    }

    // --- TRANSFERÊNCIA ---
    @PostMapping("/transfer")
    @Operation(summary = "Realizar Transferência (P2P)", description = "Move fundos entre contas. Exige Header Idempotency-Key.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Transferência realizada"),
        @ApiResponse(responseCode = "422", description = "Saldo insuficiente", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "400", description = "Dados inválidos ou Idempotency-Key faltando")
    })
    public ResponseEntity<TransactionIdResponse> transfer(
            @RequestBody TransferRequestDTO request,
            @Parameter(description = "Chave de Idempotência (UUID ou String única)", required = true)
            @RequestHeader(value = "Idempotency-Key") String idempotencyKey) {

        TransferFundsCommand command = new TransferFundsCommand(
                request.getFromAccountId(),
                request.getToAccountId(),
                request.getAmount(),
                request.getDescription(),
                idempotencyKey);

        UUID transactionId = transferFundsUseCase.execute(command);

        return ResponseEntity.ok(new TransactionIdResponse(transactionId));
    }

    // --- EXTRATO ---
    @GetMapping("/{accountId}/statement")
    @Operation(summary = "Obter Extrato", description = "Retorna o histórico de movimentações paginado.")
    public ResponseEntity<Page<StatementEntryResponse>> getStatement(
            @PathVariable UUID accountId,
            @Parameter(description = "Configuração de paginação (page, size, sort)")
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        
        Page<StatementEntryResponse> statement = accountQueryService.getStatement(accountId, pageable);
        return ResponseEntity.ok(statement);
    }

    // DTO simples para resposta de ID (pode manter aqui ou mover se quiser)
    public record TransactionIdResponse(UUID transactionId) {}
}