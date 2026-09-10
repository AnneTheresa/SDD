package com.group1.banking.dto.chat;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ChatQueryResponse {

    @JsonProperty("response")
    private String response;

    /** Plain-language basis for the answer, e.g. "Your recent transaction history". */
    @JsonProperty("based_on")
    private List<String> basedOn;

    /** True when the response falls back to general (non-personalized) guidance. */
    @JsonProperty("limited_data")
    private boolean limitedData;

    /** True when the question was refused by the guardrail (out-of-scope topic). */
    @JsonProperty("blocked")
    private boolean blocked;

    @JsonProperty("pending_confirmation")
    private PendingConfirmationView pendingConfirmation;

    public ChatQueryResponse() {}

    public ChatQueryResponse(String response, List<String> basedOn, boolean limitedData, boolean blocked) {
        this.response = response;
        this.basedOn = basedOn;
        this.limitedData = limitedData;
        this.blocked = blocked;
    }

    public String getResponse() { return response; }
    public void setResponse(String response) { this.response = response; }

    public List<String> getBasedOn() { return basedOn; }
    public void setBasedOn(List<String> basedOn) { this.basedOn = basedOn; }

    public boolean isLimitedData() { return limitedData; }
    public void setLimitedData(boolean limitedData) { this.limitedData = limitedData; }

    public boolean isBlocked() { return blocked; }
    public void setBlocked(boolean blocked) { this.blocked = blocked; }

    public PendingConfirmationView getPendingConfirmation() { return pendingConfirmation; }
    public void setPendingConfirmation(PendingConfirmationView pendingConfirmation) { this.pendingConfirmation = pendingConfirmation; }
}
