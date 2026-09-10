package com.group1.banking.exception;

public class NotFoundException extends ApiException {
    public NotFoundException(String code, String message, Object details) {
        super(404, code, message, details);
    }
}
