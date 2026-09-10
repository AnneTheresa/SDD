package com.group1.banking.entity;

/**
 * Canonical audit event types for `audit_log.event_type`.
 */
public enum AuditEventType {
    ACCOUNT_CREATED,
    ACCOUNT_DELETED,
    ACCOUNT_FROZEN,
    ACCOUNT_UNFROZEN,
    DEPOSIT_MADE,
    WITHDRAWAL_MADE,
    CHATBOT_QUERY_ANSWERED,
    USER_DELETED,
    INTEREST_RATE_UPDATED,
    FUNDS_TRANSFERRED,
    TRANSACTION_HISTORY_EXPORTED,
    STATEMENT_DOWNLOADED,
    PROFILE_EDITED,
    SAVINGS_GOAL_CREATED,
    UPDATE_SAVINGS_GOAL,
    DELETE_SAVINGS_GOAL,
    GIC_CREATED,
    GIC_REDEEMED,
    STANDING_ORDER_EXECUTED,
    STANDING_ORDER_CREATE,
    STANDING_ORDER_CANCEL,
    STANDING_ORDER_LIST,
    TRANSACTION_HISTORY_VIEWED,
    TRANSACTION_HISTORY_DENIED,
    INSIGHTS_READ,
    TRANSACTION_RECATEGORISE,
    NOTIFICATION_EVALUATE,
    NOTIFICATION_FAILED,
    NOTIFICATION_INTERNAL_FAILED,
    LOGIN,
    OTHER;


    public static AuditEventType fromString(String s) {
        if (s == null) return OTHER;
        String key = s.trim().toUpperCase().replaceAll("[^A-Z0-9]", "_");
        try {
            return AuditEventType.valueOf(key);
        } catch (IllegalArgumentException e) {
            // Try some common aliases
            switch (key) {
                case "GIC_CREATED": return GIC_CREATED;
                case "GIC_REDEEMED": return GIC_REDEEMED;
                case "STANDING_ORDER_EXECUTED": return STANDING_ORDER_EXECUTED;
                case "NOTIFICATION_FAILED": return NOTIFICATION_FAILED;
                case "NOTIFICATION_INTERNAL_FAILED": return NOTIFICATION_INTERNAL_FAILED;
                case "DEPOSIT": return DEPOSIT_MADE;
                case "WITHDRAW": return WITHDRAWAL_MADE;
                case "TRANSFER": return FUNDS_TRANSFERRED;
                case "STATEMENT_GENERATED": return STATEMENT_DOWNLOADED;
                case "CREATE_SAVINGS_GOAL": return SAVINGS_GOAL_CREATED;
                case "UPDATE_SAVINGS_GOAL": return UPDATE_SAVINGS_GOAL;
                case "DELETE_SAVINGS_GOAL": return DELETE_SAVINGS_GOAL;
                case "STANDING_ORDER_CREATE": return STANDING_ORDER_CREATE;
                case "STANDING_ORDER_LIST": return STANDING_ORDER_LIST;
                case "STANDING_ORDER_CANCEL": return STANDING_ORDER_CANCEL;
                case "TRANSACTION_HISTORY_VIEWED": return TRANSACTION_HISTORY_VIEWED;
                case "TRANSACTION_HISTORY": return TRANSACTION_HISTORY_VIEWED;
                case "TRANSACTION_HISTORY_DENIED": return TRANSACTION_HISTORY_DENIED;
                case "TRANSACTION_HISTORY_FAILED": return TRANSACTION_HISTORY_DENIED;
                case "INSIGHTS_READ": return INSIGHTS_READ;
                case "TRANSACTION_RECATEGORISE": return TRANSACTION_RECATEGORISE;
                default: return OTHER;
            }
        }
    }
}
