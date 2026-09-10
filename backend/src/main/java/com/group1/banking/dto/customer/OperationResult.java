package com.group1.banking.dto.customer;

import org.springframework.http.HttpStatus;

public record OperationResult(HttpStatus status, Object body) {}
