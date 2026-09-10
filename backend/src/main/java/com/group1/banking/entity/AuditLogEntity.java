package com.group1.banking.entity;

import jakarta.persistence.*;
import com.group1.banking.enums.RoleName;
import java.time.LocalDateTime;

/**
 * Audit log entity. (T012)
 * Table: audit_log
 */
@Entity
@Table(name = "audit_log", indexes = {
    @Index(name = "idx_al_actor_id", columnList = "actor_id"),
    @Index(name = "idx_al_subject", columnList = "subject_type, subject_id"),
    @Index(name = "idx_al_timestamp", columnList = "timestamp")
})
public class AuditLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "log_id")
    private Long logId;

    @Column(name = "event_type", nullable = false, length = 60)
    @Enumerated(EnumType.STRING)
    private AuditEventType eventType;

    @Column(name = "source_feature", length = 100)
    private String sourceFeature;

    @Column(name = "actor_type", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private RoleName actorType;

    @Column(name = "actor_id", nullable = false, length = 50)
    private String actorId;

    @Column(name = "subject_type", nullable = false, length = 60)
    private String subjectType;

    @Column(name = "subject_id", length = 100)
    private String subjectId;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private AuditOutcome outcome;

    @Column(name = "event_details", columnDefinition = "TEXT")
    private String eventDetails;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @PrePersist
    protected void onCreate() {
        timestamp = LocalDateTime.now();
    }

    public Long getLogId() { return logId; }
    public void setLogId(Long logId) { this.logId = logId; }
    public com.group1.banking.entity.AuditEventType getEventType() { return eventType; }
    public void setEventType(com.group1.banking.entity.AuditEventType eventType) { this.eventType = eventType; }
    public String getSourceFeature() { return sourceFeature; }
    public void setSourceFeature(String sourceFeature) { this.sourceFeature = sourceFeature; }
    public String getActorId() { return actorId; }
    public void setActorId(String actorId) { this.actorId = actorId; }
    public RoleName getActorType() { return actorType; }
    public void setActorType(RoleName actorType) { this.actorType = actorType; }
    public String getSubjectType() { return subjectType; }
    public void setSubjectType(String subjectType) { this.subjectType = subjectType; }
    public String getSubjectId() { return subjectId; }
    public void setSubjectId(String subjectId) { this.subjectId = subjectId; }
    public AuditOutcome getOutcome() { return outcome; }
    public void setOutcome(AuditOutcome outcome) { this.outcome = outcome; }
    public String getEventDetails() { return eventDetails; }
    public void setEventDetails(String eventDetails) { this.eventDetails = eventDetails; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}
