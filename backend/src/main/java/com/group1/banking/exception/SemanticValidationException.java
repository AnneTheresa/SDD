package com.group1.banking.exception;

public class SemanticValidationException extends RuntimeException {
    private final String code;
    private final String field;
    public SemanticValidationException(String message, String code, String field) {
        super(message);
        this.code = code;
        this.field = field;
    }
    public String getCode() { return code; }
    public String getField() { return field; }
}
