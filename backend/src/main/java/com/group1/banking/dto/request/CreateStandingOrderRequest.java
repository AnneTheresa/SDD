package com.group1.banking.dto.request;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Request DTO for creating a standing order. (T047)
 */
public class CreateStandingOrderRequest {  

    // @NotBlank(message = "Payee account is required")
    // @Size(max = 34, message = "Payee account must not exceed 34 characters")
    private Long payeeAccount;

    @NotBlank(message = "Payee name is required")
    @Size(min = 1, max = 70, message = "Payee name must be 1–70 characters")
    private String payeeName;

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private BigDecimal amount;

    @NotNull(message = "Frequency is required")
    private String frequency;

    @NotNull(message = "Start date is required")
    private LocalDateTime startDate;

    private LocalDateTime endDate;

    @NotBlank(message = "Reference is required")
    @Size(min = 1, max = 18, message = "Reference must be 1–18 characters")
    @Pattern(regexp = "[A-Za-z0-9]+", message = "Reference must be alphanumeric")
    private String reference;

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
}
