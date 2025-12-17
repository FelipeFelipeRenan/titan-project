package com.titan.ledger.adapter.in.web;

import com.titan.ledger.core.usecase.RevertTransactionUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/transactions")
public class TransactionController {

    private final RevertTransactionUseCase revertTransactionUseCase;

    public TransactionController(RevertTransactionUseCase revertTransactionUseCase) {
        this.revertTransactionUseCase = revertTransactionUseCase;
    }

    @PostMapping("/{id}/revert")
    public ResponseEntity<Void> revertTransaction(@PathVariable UUID id, @RequestBody Map<String, String> body) {
        String reason = body.getOrDefault("reason", "Administrative Reversal");
        
        revertTransactionUseCase.execute(id, reason);
        
        return ResponseEntity.ok().build();
    }
}