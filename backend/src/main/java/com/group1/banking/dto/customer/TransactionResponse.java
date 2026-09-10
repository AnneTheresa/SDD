package com.group1.banking.dto.customer;

import java.math.BigDecimal;
import java.time.Instant;

import com.group1.banking.entity.Transaction;
import com.group1.banking.entity.TransactionDirection;
import com.group1.banking.entity.TransactionStatus;

public record TransactionResponse(
        String transactionId,
        Long accountId,
        BigDecimal amount,
        TransactionDirection direction,
        TransactionStatus status,
        Instant timestamp,
        String description,
        String senderInfo,
        String receiverInfo,
        String idempotencyKey) {

    public static TransactionResponse from(Transaction transaction) {
        return new TransactionResponse(
                transaction.getTransactionId(),
                transaction.getAccount().getAccountId(),
                transaction.getAmount(),
                transaction.getDirection(),
                transaction.getStatus(),
                transaction.getTimestamp(),
                transaction.getDescription(),
                transaction.getSenderInfo(),
                transaction.getReceiverInfo(),
                transaction.getIdempotencyKey());
    }
}
