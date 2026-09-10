package com.group1.banking.dto.response;

/**
 * Cancel standing order response DTO. (T048)
 */
public class CancelStandingOrderResponse {
    private String standingOrderId;
    private String status;
    private String message;

    public CancelStandingOrderResponse() {}

    public CancelStandingOrderResponse(String standingOrderId, String status, String message) {
        this.standingOrderId = standingOrderId;
        this.status = status;
        this.message = message;
    }

    public String getStandingOrderId() { return standingOrderId; }
    public void setStandingOrderId(String standingOrderId) { this.standingOrderId = standingOrderId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
