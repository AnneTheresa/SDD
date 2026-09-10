package com.group1.banking.exception;

public class UnauthorisedException extends ApiException {
    public UnauthorisedException(String code, String message) {
        super(401, code, message, null);
    }
    
}
