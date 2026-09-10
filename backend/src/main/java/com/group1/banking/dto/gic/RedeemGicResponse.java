package com.group1.banking.dto.gic;

import java.math.BigDecimal;

public record RedeemGicResponse(String message, BigDecimal payoutAmount) {
}
