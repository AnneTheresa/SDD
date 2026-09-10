package com.group1.banking.dto.customer;

public record TransferResponse(
        String message,
        AccountResponse fromAccount,
        AccountResponse toAccount,
        TransactionResponse debitTransaction,
        TransactionResponse creditTransaction) {}
