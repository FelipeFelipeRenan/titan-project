package com.titan.ledger.adapter.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public class CreateAccountRequestDTO {
    @Schema(description = "ID único do cliente no sistema externo", example = "client-123-abc")
    private String clientId;

    public CreateAccountRequestDTO() {}

    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }
}