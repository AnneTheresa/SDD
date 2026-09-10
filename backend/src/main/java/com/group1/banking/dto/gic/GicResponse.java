package com.group1.banking.dto.gic;

import com.group1.banking.entity.GicInvestment;
import com.group1.banking.entity.GicStatus;
import com.group1.banking.entity.GicTerm;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record GicResponse(
        String gicId,
        Long accountId,
        BigDecimal principalAmount,
        BigDecimal interestRate,
        GicTerm term,
        LocalDate startDate,
        LocalDate maturityDate,
        BigDecimal maturityAmount,
        GicStatus status,
        Instant deletedAt,
        Instant createdAt,
        Instant updatedAt) {

    public static GicResponse from(GicInvestment gic) {
        return new GicResponse(
                gic.getGicId(),
                gic.getAccount().getAccountId(),
                gic.getPrincipalAmount(),
                gic.getInterestRate(),
                gic.getTerm(),
                gic.getStartDate(),
                gic.getMaturityDate(),
                gic.getMaturityAmount(),
                gic.getStatus(),
                gic.getDeletedAt(),
                gic.getCreatedAt(),
                gic.getUpdatedAt());
    }
}
