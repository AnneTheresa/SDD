package com.group1.banking.entity;

/**
 * Allowed outcomes for audit events.
 */
public enum AuditOutcome {
    SUCCESS,
    DENIED,
    ERROR;

    public static AuditOutcome fromString(String s) {
        if (s == null) return ERROR;
        String key = s.trim().toUpperCase().replaceAll("[^A-Z0-9]", "_");
        try {
            return AuditOutcome.valueOf(key);
        } catch (IllegalArgumentException e) {
            return ERROR;
        }
    }
}
