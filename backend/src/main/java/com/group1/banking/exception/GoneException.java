package com.group1.banking.exception;

public class GoneException extends ApiException {
    public GoneException(String code, String message, Object details) {
        super(410, code, message, details);
    }
}
