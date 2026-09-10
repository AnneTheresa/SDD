package com.group1.banking.dto.customer;

public record MonetaryOperationResponse(String message, AccountResponse account, TransactionResponse transaction) {}
