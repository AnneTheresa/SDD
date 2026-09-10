package com.group1.banking.dto.chat;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

/** The `pending_confirmation` block on a chat response - present only on a turn where
 * the agent proposed (but did not execute) a financial/account-changing action. */
public class PendingConfirmationView {

    @JsonProperty("token")
    private String token;

    @JsonProperty("action_type")
    private String actionType;

    @JsonProperty("summary")
    private String summary;

    @JsonProperty("expires_at")
    private LocalDateTime expiresAt;

    public PendingConfirmationView() {}

    public PendingConfirmationView(String token, String actionType, String summary, LocalDateTime expiresAt) {
        this.token = token;
        this.actionType = actionType;
        this.summary = summary;
        this.expiresAt = expiresAt;
    }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public String getActionType() { return actionType; }
    public void setActionType(String actionType) { this.actionType = actionType; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
}
