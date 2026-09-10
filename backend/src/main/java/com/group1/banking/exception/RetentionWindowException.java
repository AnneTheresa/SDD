package com.group1.banking.exception;

public class RetentionWindowException extends RuntimeException {
    private final String code;
    public RetentionWindowException(String message, String code) {
        super(message);
        this.code = code;
    }
    public String getCode() { return code; }
}
