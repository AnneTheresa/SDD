package com.group1.banking.entity;

/**
 * Standing order lifecycle states. (T044)
 */
public enum StandingOrderStatus {
    ACTIVE,
    CANCELLED,
    LOCKED,
    TERMINATED,
    RETRY_PENDING,
    FAILED_INSUFFICIENT_FUNDS
}
