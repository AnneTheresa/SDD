package com.group1.banking.dto.chat;

import java.math.BigDecimal;

public record AccountSummary(Long accountId, String accountType, String status, BigDecimal balance) {
}
