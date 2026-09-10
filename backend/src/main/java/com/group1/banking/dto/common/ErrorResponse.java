package com.group1.banking.dto.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
public class ErrorResponse {
	private String code;
    private String message;
    private Object details; // flexible: String OR Map

    public ErrorResponse(String code, String message, Object details) {
        this.code = code;
        this.message = message;
        this.details = details;
    }
}
