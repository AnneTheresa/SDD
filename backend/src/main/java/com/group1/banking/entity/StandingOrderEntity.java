package com.group1.banking.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Standing order entity. (T045)
 * Table: standing_orders
 */
@Entity
@Table(name = "standing_orders", indexes = {
        @Index(name = "idx_so_source_account_id", columnList = "source_account_id"),
        @Index(name = "idx_so_status", columnList = "status"),
        @Index(name = "idx_so_next_run_date", columnList = "next_run_date")
})
public class StandingOrderEntity {

    @Id
    @Column(name = "standing_order_id", length = 36)
    private String standingOrderId;

    @Column(name = "source_account_id", nullable = false)
    private Long sourceAccountId;

    @Column(name = "payee_account", nullable = false, length = 34)
    private Long payeeAccount;

    @Column(name = "payee_name", nullable = false, length = 70)
    private String payeeName;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Frequency frequency;

    @Column(name = "start_date", nullable = false)
    private LocalDateTime startDate;

    @Column(name = "end_date")
    private LocalDateTime endDate;

    @Column(nullable = false, length = 18)
    private String reference;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StandingOrderStatus status;

    @Column(name = "next_run_date", nullable = false)
    private LocalDateTime nextRunDate;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

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
    public Frequency getFrequency() { return frequency; }
    public void setFrequency(Frequency frequency) { this.frequency = frequency; }
    public LocalDateTime getStartDate() { return startDate; }
    public void setStartDate(LocalDateTime startDate) { this.startDate = startDate; }
    public LocalDateTime getEndDate() { return endDate; }
    public void setEndDate(LocalDateTime endDate) { this.endDate = endDate; }
    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }
    public StandingOrderStatus getStatus() { return status; }
    public void setStatus(StandingOrderStatus status) { this.status = status; }
    public LocalDateTime getNextRunDate() { return nextRunDate; }
    public void setNextRunDate(LocalDateTime nextRunDate) { this.nextRunDate = nextRunDate; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
