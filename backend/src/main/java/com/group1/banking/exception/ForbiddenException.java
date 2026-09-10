package com.group1.banking.exception;

public class ForbiddenException extends ApiException {
    public ForbiddenException(String code, String message) {
        super(403, code, message, null);
    }
}
