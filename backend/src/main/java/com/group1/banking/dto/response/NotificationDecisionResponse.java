package com.group1.banking.dto.response;

import java.time.LocalDateTime;

/**
 * Notification decision response DTO. (T072)
 */
public class NotificationDecisionResponse {
    private String eventId;
    private String decision;
    private String decisionReason;
    private Long customerId;
    private Long accountId;
    private LocalDateTime evaluatedAt;
    private boolean mandatoryOverride;

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }
    public String getDecision() { return decision; }
    public void setDecision(String decision) { this.decision = decision; }
    public String getDecisionReason() { return decisionReason; }
    public void setDecisionReason(String decisionReason) { this.decisionReason = decisionReason; }
    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }
    public Long getAccountId() { return accountId; }
    public void setAccountId(Long accountId) { this.accountId = accountId; }
    public LocalDateTime getEvaluatedAt() { return evaluatedAt; }
    public void setEvaluatedAt(LocalDateTime evaluatedAt) { this.evaluatedAt = evaluatedAt; }
    public boolean isMandatoryOverride() { return mandatoryOverride; }
    public void setMandatoryOverride(boolean mandatoryOverride) { this.mandatoryOverride = mandatoryOverride; }
}
