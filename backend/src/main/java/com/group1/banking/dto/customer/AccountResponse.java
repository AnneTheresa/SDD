package com.group1.banking.dto.customer;

import java.math.BigDecimal;
import java.time.Instant;

import com.group1.banking.entity.Account;
import com.group1.banking.entity.AccountStatus;
import com.group1.banking.entity.AccountType;

public record AccountResponse(
        Long accountId,
        Long customerId,
        AccountType accountType,
        AccountStatus status,
        BigDecimal balance,
        BigDecimal interestRate,
        Instant createdAt,
        Instant updatedAt) {

    public static AccountResponse from(Account account) {
        return new AccountResponse(
                account.getAccountId(),
                account.getCustomer().getCustomerId(),
                account.getAccountType(),
                account.getStatus(),
                account.getBalance(),
                account.getInterestRate(),
                account.getCreatedAt(),
                account.getUpdatedAt());
    }
}
