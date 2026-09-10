package com.group1.banking.exception;

public class ConflictException extends ApiException {
    private static final long serialVersionUID = 1L;

    public ConflictException(String code, String message, Object details) {
        super(409, code, message, details);
    }
}
