package com.group1.banking.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Notification decision entity. (T069)
 * Table: notification_decisions
 */
@Entity
@Table(name = "notification_decisions", indexes = {
        @Index(name = "idx_nd_account_id", columnList = "account_id"),
        @Index(name = "idx_nd_customer_id", columnList = "customer_id"),
        @Index(name = "idx_nd_event_type", columnList = "event_type")
})
public class NotificationDecisionEntity {

    @Id
    @Column(name = "event_id", length = 36)
    private String eventId;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "business_timestamp", nullable = false)
    private LocalDateTime businessTimestamp;

    @Column(columnDefinition = "TEXT")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationDecision decision;

    @Column(name = "decision_reason", nullable = false, length = 500)
    private String decisionReason;

    @Column(name = "mandatory_override", nullable = false)
    private boolean mandatoryOverride = false;

    @Column(name = "evaluated_at", nullable = false)
    private LocalDateTime evaluatedAt;

    @PrePersist
    protected void onCreate() {
        evaluatedAt = LocalDateTime.now();
    }

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public Long getAccountId() { return accountId; }
    public void setAccountId(Long accountId) { this.accountId = accountId; }
    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }
    public LocalDateTime getBusinessTimestamp() { return businessTimestamp; }
    public void setBusinessTimestamp(LocalDateTime businessTimestamp) { this.businessTimestamp = businessTimestamp; }
    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }
    public NotificationDecision getDecision() { return decision; }
    public void setDecision(NotificationDecision decision) { this.decision = decision; }
    public String getDecisionReason() { return decisionReason; }
    public void setDecisionReason(String decisionReason) { this.decisionReason = decisionReason; }
    public boolean isMandatoryOverride() { return mandatoryOverride; }
    public void setMandatoryOverride(boolean mandatoryOverride) { this.mandatoryOverride = mandatoryOverride; }
    public LocalDateTime getEvaluatedAt() { return evaluatedAt; }
    public void setEvaluatedAt(LocalDateTime evaluatedAt) { this.evaluatedAt = evaluatedAt; }
}
