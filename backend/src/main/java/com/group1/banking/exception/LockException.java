package com.group1.banking.exception;

public class LockException extends RuntimeException {
    private final String code;
    public LockException(String message, String code) {
        super(message);
        this.code = code;
    }
    public String getCode() { return code; }
}
