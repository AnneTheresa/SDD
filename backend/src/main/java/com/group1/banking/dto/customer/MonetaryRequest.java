package com.group1.banking.dto.customer;

import java.math.BigDecimal;

public record MonetaryRequest(BigDecimal amount, String description) {}
