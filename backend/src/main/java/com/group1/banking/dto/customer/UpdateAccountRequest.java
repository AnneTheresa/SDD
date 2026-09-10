package com.group1.banking.dto.customer;

import jakarta.validation.constraints.Digits;
import java.math.BigDecimal;

public record UpdateAccountRequest(
        @Digits(integer = 8, fraction = 4) BigDecimal interestRate) {

    public boolean isEmpty() {
        return interestRate == null;
    }
}
