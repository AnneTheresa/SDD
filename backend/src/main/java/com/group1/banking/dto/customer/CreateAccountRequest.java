package com.group1.banking.dto.customer;

import java.math.BigDecimal;

import com.group1.banking.entity.AccountType;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

public record CreateAccountRequest(
        @NotNull AccountType accountType,
        
        @NotNull @DecimalMin(value = "0.00") 
        @Digits(integer = 17, fraction = 2) 
        BigDecimal balance,
        
        @DecimalMin(value = "0.0", inclusive = true) 
        @Digits(integer = 8, fraction = 4) 
        BigDecimal interestRate) {
	
	
}
