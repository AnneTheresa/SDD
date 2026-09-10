package com.group1.banking.exception;

public class UnprocessableException extends BankingException {

    public UnprocessableException(String code, String message, String field) {
        super(code, message, field);
    }
}
