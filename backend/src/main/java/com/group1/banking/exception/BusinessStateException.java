package com.group1.banking.exception;

public class BusinessStateException extends RuntimeException {
    private final String code;
    private final String field;
    public BusinessStateException(String message, String code, String field) {
        super(message);
        this.code = code;
        this.field = field;
    }
    public BusinessStateException(String message, String code) {
        this(message, code, null);
    }
    public String getCode() { return code; }
    public String getField() { return field; }
}
