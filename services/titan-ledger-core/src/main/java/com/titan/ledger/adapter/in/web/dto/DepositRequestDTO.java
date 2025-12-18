package com.titan.ledger.adapter.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

public class DepositRequestDTO {
    @Schema(description = "Valor do depósito", example = "500.00", type = "number", format = "double")
    private BigDecimal amount;
    
    @Schema(description = "Descrição da origem do dinheiro", example = "Depósito via ATM")
    private String description;

    public DepositRequestDTO() {} // Obrigatório

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}