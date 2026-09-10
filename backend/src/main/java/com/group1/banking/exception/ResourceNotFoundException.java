package com.group1.banking.exception;

import java.util.Map;

public class ResourceNotFoundException extends RuntimeException {
    private final String code;
    private final Map<String, String> details;
    public ResourceNotFoundException(String message, String code, Map<String, String> details) {
        super(message);
        this.code = code;
        this.details = details != null ? Map.copyOf(details) : null;
    }
    public String getCode() { return code; }
    public Map<String, String> getDetails() {
        return details;
    }
}
