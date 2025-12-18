package com.titan.ledger.adapter.in.web;

import com.titan.ledger.core.usecase.RevertTransactionUseCase;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/transactions")
@Tag(name = "Transactions", description = "Endpoints para gestão e estorno de transações")
public class TransactionController {

    private final RevertTransactionUseCase revertTransactionUseCase;

    public TransactionController(RevertTransactionUseCase revertTransactionUseCase) {
        this.revertTransactionUseCase = revertTransactionUseCase;
    }

    @PostMapping("/{id}/revert")
    @Operation(summary = "Estornar uma transação", description = "Cria uma transação reversa para anular o efeito financeiro da original. A original é marcada como REVERTED.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Estorno realizado com sucesso"),
            @ApiResponse(responseCode = "404", description = "Transação original não encontrada", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "400", description = "Transação já foi estornada ou não pode ser estornada", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<Void> revertTransaction(@PathVariable UUID id,
            @RequestBody @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Motivo do estorno", content = @Content(examples = @ExampleObject(value = "{\"reason\": \"Erro operacional\"}"))) Map<String, String> body) {
        String reason = body.getOrDefault("reason", "Administrative Reversal");

        revertTransactionUseCase.execute(id, reason);

        return ResponseEntity.ok().build();
    }
}