package com.group1.banking.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Standing order response DTO. (T048)
 */
public class StandingOrderResponse {
    private String standingOrderId;
    private Long sourceAccountId;
    private Long payeeAccount;
    private String payeeName;
    private BigDecimal amount;
    private String frequency;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String reference;
    private String status;
    private LocalDateTime nextRunDate;
    private String message;

    public String getStandingOrderId() { return standingOrderId; }
    public void setStandingOrderId(String standingOrderId) { this.standingOrderId = standingOrderId; }
    public Long getSourceAccountId() { return sourceAccountId; }
    public void setSourceAccountId(Long sourceAccountId) { this.sourceAccountId = sourceAccountId; }
    public Long getPayeeAccount() { return payeeAccount; }
    public void setPayeeAccount(Long payeeAccount) { this.payeeAccount = payeeAccount; }
    public String getPayeeName() { return payeeName; }
    public void setPayeeName(String payeeName) { this.payeeName = payeeName; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getFrequency() { return frequency; }
    public void setFrequency(String frequency) { this.frequency = frequency; }
    public LocalDateTime getStartDate() { return startDate; }
    public void setStartDate(LocalDateTime startDate) { this.startDate = startDate; }
    public LocalDateTime getEndDate() { return endDate; }
    public void setEndDate(LocalDateTime endDate) { this.endDate = endDate; }
    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getNextRunDate() { return nextRunDate; }
    public void setNextRunDate(LocalDateTime nextRunDate) { this.nextRunDate = nextRunDate; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
