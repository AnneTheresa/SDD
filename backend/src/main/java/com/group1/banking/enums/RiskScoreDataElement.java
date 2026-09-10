package com.group1.banking.enums;

public enum RiskScoreDataElement {
    SPENDING_INCOME_RATIO,
    SAVING_BALANCE,
    GOAL_PROGRESS,
    // Not a weighted input: recorded as a triggered factor when a frozen account
    // forces the band to HIGH, so the override is auditable rather than silent.
    FROZEN_ACCOUNT_OVERRIDE
}
