package com.titan.ledger.adapter.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "Dados para realizar uma transferência entre contas")
public class TransferRequestDTO {
    
    @Schema(description = "UUID da conta que enviará o dinheiro (Débito)", example = "80d5f08a-03e3-486f-8fd0-b6584a9c19f1")
    private UUID fromAccountId;

    @Schema(description = "UUID da conta que receberá o dinheiro (Crédito)", example = "e2294b06-d0a0-4713-a4b8-e6c4fbc9f0cb")
    private UUID toAccountId;

    @Schema(description = "Valor a ser transferido. Deve ser positivo e maior que zero.", example = "100.50", type = "number", format = "double")
    private BigDecimal amount;

    @Schema(description = "Descrição curta da transação para o extrato", example = "Pagamento do Aluguel")
    private String description;

    public TransferRequestDTO() {}

    // Getters e Setters (Mantenha os que já existem)
    public UUID getFromAccountId() { return fromAccountId; }
    public void setFromAccountId(UUID fromAccountId) { this.fromAccountId = fromAccountId; }
    public UUID getToAccountId() { return toAccountId; }
    public void setToAccountId(UUID toAccountId) { this.toAccountId = toAccountId; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}