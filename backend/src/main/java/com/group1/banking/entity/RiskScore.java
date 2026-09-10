package com.group1.banking.entity;

import java.time.Instant;

import com.group1.banking.enums.RiskScoreLevel;
import com.group1.banking.enums.RiskScoreStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor

@Entity
@Table(name = "risk_scores", indexes = {
        @Index(name = "idx_customer_score_created", columnList = "customer_id, calculatedAt"),

})
public class RiskScore {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "rule_version")
    private Double version;

    @Column(name = "score")
    private Double score;

    @Enumerated(EnumType.STRING)
    @Column(name = "band")
    private RiskScoreLevel riskLevel;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private RiskScoreStatus calculateStatus;

    @Column(name = "factors", length = 2000)
    private String factors;

    @Column(name = "calculatedAt")
    private Instant calculatedAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @PrePersist
    public void prePersist() {
        Instant now = Instant.now();
        this.calculatedAt = now;
    }

}
