package com.group1.banking.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * A financial or account-changing action the chat agent proposed but has not
 * executed - AC4's "confirm before completing" gate. Persisted on the primary
 * datasource (not the chatbot's separate Postgres/pgvector store) since this is
 * account/transfer domain data. See ConfirmationGateService.
 */
@Entity
@Table(name = "pending_agent_actions", indexes = {
        @Index(name = "idx_paa_customer_id", columnList = "customer_id"),
        @Index(name = "idx_paa_status", columnList = "status")
})
public class PendingAgentActionEntity {

    @Id
    @Column(name = "token", length = 36)
    private String token;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 20)
    private PendingAgentActionType actionType;

    @Column(name = "parameters_json", nullable = false, columnDefinition = "TEXT")
    private String parametersJson;

    @Column(name = "human_summary", nullable = false, columnDefinition = "TEXT")
    private String humanSummary;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PendingAgentActionStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }
    public PendingAgentActionType getActionType() { return actionType; }
    public void setActionType(PendingAgentActionType actionType) { this.actionType = actionType; }
    public String getParametersJson() { return parametersJson; }
    public void setParametersJson(String parametersJson) { this.parametersJson = parametersJson; }
    public String getHumanSummary() { return humanSummary; }
    public void setHumanSummary(String humanSummary) { this.humanSummary = humanSummary; }
    public PendingAgentActionStatus getStatus() { return status; }
    public void setStatus(PendingAgentActionStatus status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
}
