package com.group1.banking.dto.response;

import java.time.LocalDateTime;

/**
 * Standard error response body. (T007)
 * Fields: code, message, field (optional, present when error relates to a specific field).
 */
public class ErrorResponse {

    private String code;
    private String message;
    private String field;
    private LocalDateTime timestamp;

    public ErrorResponse() {}

    public ErrorResponse(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public ErrorResponse(String code, String message, String field) {
        this.code = code;
        this.message = message;
        this.field = field;
    }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getField() { return field; }
    public void setField(String field) { this.field = field; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}
