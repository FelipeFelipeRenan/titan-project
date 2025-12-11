package com.titan.ledger.core.domain.exception;

public class InsufficientFundsException extends RuntimeException{
    public  InsufficientFundsException(String message){
        super(message);
    }
}
