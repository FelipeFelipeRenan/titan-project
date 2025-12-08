package com.titan.ledger.core.usecase.dto;

public record CreateAccountCommand(
    String clientId,
    String currency
){
    public CreateAccountCommand{
        if(clientId == null || clientId.isBlank()){
            throw new IllegalArgumentException("Client ID cannot be empty");
        }
        
        if(currency == null || currency.length() != 3){
            throw new IllegalArgumentException("Currency must be a a 3-letter ISO code");
        }

    
        }
}