package com.group1.banking.dto.chat;

import java.math.BigDecimal;
import java.util.Map;

public record SpendCategorySummary(
        Map<String, BigDecimal> byCategory,
        int transactionCount,
        int lookbackDays,
        boolean sufficientData) {
}
