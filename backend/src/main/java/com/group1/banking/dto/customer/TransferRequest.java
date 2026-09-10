package com.group1.banking.dto.customer;

import java.math.BigDecimal;

public record TransferRequest(Long fromAccountId, Long toAccountId, BigDecimal amount, String description) {}
