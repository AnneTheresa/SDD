package com.group1.banking.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PdfStatementServiceTest {

    private PdfStatementService pdfStatementService;

    @BeforeEach
    void setUp() {
        pdfStatementService = new PdfStatementService();
    }

    @Test
    void buildStatementPdf_shouldReturnNonNullByteArray() {
        byte[] result = buildSamplePdf();
        assertThat(result).isNotNull();
    }

    @Test
    void buildStatementPdf_shouldReturnNonEmptyByteArray() {
        byte[] result = buildSamplePdf();
        assertThat(result.length).isGreaterThan(0);
    }

    @Test
    void buildStatementPdf_shouldProduceValidPdfMagicBytes() {
        byte[] result = buildSamplePdf();
        // PDF files start with "%PDF"
        assertThat(new String(result, 0, 4)).isEqualTo("%PDF");
    }

    @Test
    void buildStatementPdf_shouldWorkForCurrentMonth() {
        byte[] result = pdfStatementService.buildStatementPdf(
                1001L, "CA12345678901234567890", "ACTIVE", "Alice Smith",
                YearMonth.now(), true,
                new BigDecimal("1000.00"), new BigDecimal("850.00"),
                new BigDecimal("200.00"), new BigDecimal("350.00"),
                List.of());
        assertThat(result).isNotNull().isNotEmpty();
    }

    @Test
    void buildStatementPdf_shouldWorkWithNullCustomerName() {
        byte[] result = pdfStatementService.buildStatementPdf(
                1001L, "CA12345678901234567890", "ACTIVE", null,
                YearMonth.of(2024, 1), false,
                BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO,
                List.of());
        assertThat(result).isNotNull().isNotEmpty();
    }

    @Test
    void buildStatementPdf_shouldWorkWithZeroBalances() {
        byte[] result = pdfStatementService.buildStatementPdf(
                1001L, "CA12345678901234567890", "CLOSED", "Bob Jones",
                YearMonth.of(2023, 6), false,
                BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO,
                List.of());
        assertThat(result).isNotNull().isNotEmpty();
    }

    private byte[] buildSamplePdf() {
        return pdfStatementService.buildStatementPdf(
                1001L, "CA12345678901234567890", "ACTIVE", "Alice Smith",
                YearMonth.of(2024, 1), false,
                new BigDecimal("1000.00"), new BigDecimal("900.00"),
                new BigDecimal("200.00"), new BigDecimal("300.00"),
                List.of());
    }
}
