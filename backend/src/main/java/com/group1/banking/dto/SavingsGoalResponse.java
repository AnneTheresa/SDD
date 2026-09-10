package com.group1.banking.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.group1.banking.enums.SavingsGoalStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;

/**
 * Savings Goal Response DTO
 */
public class SavingsGoalResponse {

    @JsonProperty("goal_id")
    private Long goalId;

    @JsonProperty("account_id")
    private Long accountId;

    @JsonProperty("account_number")
    private String accountNumber;

    @JsonProperty("account_type")
    private String accountType;

    @JsonProperty("goal_name")
    private String goalName;

    @JsonProperty("target_amount")
    private BigDecimal targetAmount;

    @JsonProperty("target_date")
    private LocalDate targetDate;

    @JsonProperty("current_balance")
    private BigDecimal currentBalance;

    @JsonProperty("progress_percentage")
    private BigDecimal progressPercentage;

    @JsonProperty("time_remaining_days")
    private Long timeRemainingDays;

    @JsonProperty("status")
    private SavingsGoalStatus status;

    @JsonProperty("created_at")
    private ZonedDateTime createdAt;

    @JsonProperty("updated_at")
    private ZonedDateTime updatedAt;

    public SavingsGoalResponse() {}

    public Long getGoalId() { return goalId; }
    public void setGoalId(Long goalId) { this.goalId = goalId; }

    public Long getAccountId() { return accountId; }
    public void setAccountId(Long accountId) { this.accountId = accountId; }

    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }

    public String getAccountType() { return accountType; }
    public void setAccountType(String accountType) { this.accountType = accountType; }

    public String getGoalName() { return goalName; }
    public void setGoalName(String goalName) { this.goalName = goalName; }

    public BigDecimal getTargetAmount() { return targetAmount; }
    public void setTargetAmount(BigDecimal targetAmount) { this.targetAmount = targetAmount; }

    public LocalDate getTargetDate() { return targetDate; }
    public void setTargetDate(LocalDate targetDate) { this.targetDate = targetDate; }

    public BigDecimal getCurrentBalance() { return currentBalance; }
    public void setCurrentBalance(BigDecimal currentBalance) { this.currentBalance = currentBalance; }

    public BigDecimal getProgressPercentage() { return progressPercentage; }
    public void setProgressPercentage(BigDecimal progressPercentage) { this.progressPercentage = progressPercentage; }

    public Long getTimeRemainingDays() { return timeRemainingDays; }
    public void setTimeRemainingDays(Long timeRemainingDays) { this.timeRemainingDays = timeRemainingDays; }

    public SavingsGoalStatus getStatus() { return status; }
    public void setStatus(SavingsGoalStatus status) { this.status = status; }

    public ZonedDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(ZonedDateTime createdAt) { this.createdAt = createdAt; }

    public ZonedDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(ZonedDateTime updatedAt) { this.updatedAt = updatedAt; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private final SavingsGoalResponse r = new SavingsGoalResponse();

        public Builder goalId(Long v) { r.goalId = v; return this; }
        public Builder accountId(Long v) { r.accountId = v; return this; }
        public Builder accountNumber(String v) { r.accountNumber = v; return this; }
        public Builder accountType(String v) { r.accountType = v; return this; }
        public Builder goalName(String v) { r.goalName = v; return this; }
        public Builder targetAmount(BigDecimal v) { r.targetAmount = v; return this; }
        public Builder targetDate(LocalDate v) { r.targetDate = v; return this; }
        public Builder currentBalance(BigDecimal v) { r.currentBalance = v; return this; }
        public Builder progressPercentage(BigDecimal v) { r.progressPercentage = v; return this; }
        public Builder timeRemainingDays(Long v) { r.timeRemainingDays = v; return this; }
        public Builder status(SavingsGoalStatus v) { r.status = v; return this; }
        public Builder createdAt(ZonedDateTime v) { r.createdAt = v; return this; }
        public Builder updatedAt(ZonedDateTime v) { r.updatedAt = v; return this; }

        public SavingsGoalResponse build() { return r; }
    }
}

