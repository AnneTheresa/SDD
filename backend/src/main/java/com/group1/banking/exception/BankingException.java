package com.group1.banking.exception;

public abstract class BankingException extends RuntimeException {

    private final String code;
    private final String field;

    protected BankingException(String code, String message, String field) {
        super(message);
        this.code = code;
        this.field = field;
    }

    public String getCode() {
        return code;
    }

    public String getField() {
        return field;
    }
}
