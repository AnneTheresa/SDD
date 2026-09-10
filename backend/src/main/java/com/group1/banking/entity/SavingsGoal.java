package com.group1.banking.entity;

import jakarta.persistence.*;
import com.group1.banking.enums.SavingsGoalStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Savings Goal Entity
 * 
 * Represents a customer's savings goal for a specific account.
 * One active goal per account per customer (enforced by UNIQUE constraint).
 * 
 * Progress and status are derived at read time:
 * - progress_percentage = (account.balance / target_amount) * 100, capped at 100%
 * - status = calculated from account.balance and target_date
 * 
 * Follows soft-delete pattern: deleted_at != NULL indicates inactive goal
 */
@Entity
@Table(name = "savings_goals", 
       uniqueConstraints = @UniqueConstraint(
           name = "uq_sg_customer_account", 
           columnNames = {"customer_id", "account_id"}
       ),
       indexes = {
           @Index(name = "idx_sg_customer_id", columnList = "customer_id"),
           @Index(name = "idx_sg_account_id", columnList = "account_id"),
           @Index(name = "idx_sg_status", columnList = "status")
       })
public class SavingsGoal {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "goal_id")
    private Long goalId;
    
    @Column(name = "customer_id", nullable = false)
    private Long customerId;
    
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;
    
    /**
     * User's answer to "What are you saving for?"
     * Stores preset value (e.g., "Travel") or custom free-text entry
     */
    @Column(name = "goal_name", nullable = false, length = 255)
    private String goalName;
    
    /**
     * User's answer to "How much do you plan to save?"
     * Must be > 0
     * Precision: DECIMAL(19,2) to match account.balance
     */
    @Column(name = "target_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal targetAmount;
    
    /**
     * User's answer to "By when?"
     * Target date for achieving the goal
     */
    @Column(name = "target_date", nullable = false)
    private LocalDate targetDate;
    
    /**
     * Goal status (derived on every read)
     * NOT_STARTED, IN_PROGRESS, ACHIEVED, OVERDUE
     * Stored for indexing; recalculated on read to ensure accuracy
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private SavingsGoalStatus status;
    
    /**
     * Soft delete timestamp
     * NULL = active goal
     * NOT NULL = deleted goal (remains in database for audit trail)
     */
    @Column(name = "deleted_at")
    private Instant deletedAt;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    
    /**
     * Transient field for response DTO
     * Current balance pulled from account.balance at read time
     * Not stored in database
     */
    @Transient
    private BigDecimal currentBalance;
    
    /**
     * Transient field for response DTO
     * Progress percentage: (currentBalance / targetAmount) * 100, capped at 100
     * Calculated on every read, not stored
     */
    @Transient
    private BigDecimal progressPercentage;
    
    /**
     * Transient field for response DTO
     * Time remaining in days: targetDate - TODAY
     * Never negative (capped at 0 if overdue)
     * Calculated on every read, not stored
     */
    @Transient
    private Long timeRemainingDays;
    
    public Long getGoalId() { return goalId; }
    public void setGoalId(Long goalId) { this.goalId = goalId; }

    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }

    public Account getAccount() { return account; }
    public void setAccount(Account account) { this.account = account; }

    public String getGoalName() { return goalName; }
    public void setGoalName(String goalName) { this.goalName = goalName; }

    public BigDecimal getTargetAmount() { return targetAmount; }
    public void setTargetAmount(BigDecimal targetAmount) { this.targetAmount = targetAmount; }

    public LocalDate getTargetDate() { return targetDate; }
    public void setTargetDate(LocalDate targetDate) { this.targetDate = targetDate; }

    public SavingsGoalStatus getStatus() { return status; }
    public void setStatus(SavingsGoalStatus status) { this.status = status; }

    public Instant getDeletedAt() { return deletedAt; }
    public void setDeletedAt(Instant deletedAt) { this.deletedAt = deletedAt; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public BigDecimal getCurrentBalance() { return currentBalance; }
    public void setCurrentBalance(BigDecimal currentBalance) { this.currentBalance = currentBalance; }

    public BigDecimal getProgressPercentage() { return progressPercentage; }
    public void setProgressPercentage(BigDecimal progressPercentage) { this.progressPercentage = progressPercentage; }

    public Long getTimeRemainingDays() { return timeRemainingDays; }
    public void setTimeRemainingDays(Long timeRemainingDays) { this.timeRemainingDays = timeRemainingDays; }

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
        if (status == null) {
            status = SavingsGoalStatus.NOT_STARTED;
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
