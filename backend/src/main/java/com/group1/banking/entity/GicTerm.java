package com.group1.banking.entity;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * GIC investment term options with their associated annual interest rates.
 * Interest rate is fixed per term and cannot be overridden by the caller.
 */
public enum GicTerm {

    SIX_MONTHS(new BigDecimal("0.0300"), new BigDecimal("0.5")),
    ONE_YEAR  (new BigDecimal("0.0500"), new BigDecimal("1")),
    TWO_YEARS (new BigDecimal("0.0550"), new BigDecimal("2")),
    THREE_YEARS(new BigDecimal("0.0600"), new BigDecimal("3")),
    FIVE_YEARS (new BigDecimal("0.0700"), new BigDecimal("5"));

    /** Annual interest rate (e.g. 0.05 = 5%). */
    private final BigDecimal annualRate;

    /** Duration in years (fractional for sub-year terms). */
    private final BigDecimal termYears;

    GicTerm(BigDecimal annualRate, BigDecimal termYears) {
        this.annualRate = annualRate;
        this.termYears = termYears;
    }

    public BigDecimal getAnnualRate() {
        return annualRate;
    }

    public BigDecimal getTermYears() {
        return termYears;
    }

    /** Compute the maturity date from a given start date. */
    public LocalDate computeMaturityDate(LocalDate startDate) {
        return switch (this) {
            case SIX_MONTHS  -> startDate.plusMonths(6);
            case ONE_YEAR    -> startDate.plusYears(1);
            case TWO_YEARS   -> startDate.plusYears(2);
            case THREE_YEARS -> startDate.plusYears(3);
            case FIVE_YEARS  -> startDate.plusYears(5);
        };
    }

    /**
     * Compute maturity amount: principal + (principal * annualRate * termYears).
     */
    public BigDecimal computeMaturityAmount(BigDecimal principal) {
        BigDecimal interest = principal.multiply(annualRate).multiply(termYears);
        return principal.add(interest).setScale(2, java.math.RoundingMode.HALF_UP);
    }
}
