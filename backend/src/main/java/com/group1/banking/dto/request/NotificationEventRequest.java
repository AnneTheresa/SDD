package com.group1.banking.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Request DTO for notification event evaluation. (T071)
 */
public class NotificationEventRequest {

    @NotBlank(message = "eventId is required")
    private String eventId;

    @NotBlank(message = "eventType is required")
    private String eventType;

    @NotNull(message = "accountId is required")
    @Positive(message = "accountId must be positive")
    private Long accountId;

    @NotNull(message = "customerId is required")
    @Positive(message = "customerId must be positive")
    private Long customerId;

    @NotBlank(message = "businessTimestamp is required")
    private String businessTimestamp;

    private String payload;

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public Long getAccountId() { return accountId; }
    public void setAccountId(Long accountId) { this.accountId = accountId; }
    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }
    public String getBusinessTimestamp() { return businessTimestamp; }
    public void setBusinessTimestamp(String businessTimestamp) { this.businessTimestamp = businessTimestamp; }
    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }
}
