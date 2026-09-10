package com.group1.banking.enums;

/**
 * Savings Goal Status Enum
 * 
 * Represents the current status of a savings goal.
 * Status is derived on every read from account.balance and target_date.
 */
public enum SavingsGoalStatus {
    /**
     * NOT_STARTED: No progress yet
     * Condition: account.balance = 0 AND target_date >= TODAY
     */
    NOT_STARTED,
    
    /**
     * IN_PROGRESS: Partial progress toward goal
     * Condition: account.balance > 0 AND account.balance < target_amount AND target_date >= TODAY
     */
    IN_PROGRESS,
    
    /**
     * ACHIEVED: Goal target met or exceeded
     * Condition: account.balance >= target_amount
     */
    ACHIEVED,
    
    /**
     * OVERDUE: Deadline passed without achieving goal
     * Condition: target_date < TODAY AND account.balance < target_amount
     */
    OVERDUE
}
