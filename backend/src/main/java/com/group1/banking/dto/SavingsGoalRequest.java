package com.group1.banking.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Savings Goal Request DTO
 * Used for POST /accounts/{account_id}/goals and PUT /accounts/{account_id}/goals/{goal_id}
 */
public class SavingsGoalRequest {

    @NotBlank(message = "Goal name is required")
    @Size(min = 1, max = 255, message = "Goal name must be between 1 and 255 characters")
    private String goalName;

    @NotNull(message = "Target amount is required")
    @DecimalMin(value = "0.01", message = "Target amount must be greater than $0")
    @Digits(integer = 17, fraction = 2, message = "Target amount precision error")
    private BigDecimal targetAmount;

    @NotNull(message = "Target date is required")
    @FutureOrPresent(message = "Target date must be in the future or today")
    private LocalDate targetDate;

    public SavingsGoalRequest() {}

    public SavingsGoalRequest(String goalName, BigDecimal targetAmount, LocalDate targetDate) {
        this.goalName = goalName;
        this.targetAmount = targetAmount;
        this.targetDate = targetDate;
    }

    public String getGoalName() { return goalName; }
    public void setGoalName(String goalName) { this.goalName = goalName; }

    public BigDecimal getTargetAmount() { return targetAmount; }
    public void setTargetAmount(BigDecimal targetAmount) { this.targetAmount = targetAmount; }

    public LocalDate getTargetDate() { return targetDate; }
    public void setTargetDate(LocalDate targetDate) { this.targetDate = targetDate; }
}

