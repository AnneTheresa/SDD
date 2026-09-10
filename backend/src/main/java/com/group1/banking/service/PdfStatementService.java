package com.group1.banking.service;

import com.group1.banking.entity.Transaction;
import com.group1.banking.entity.TransactionStatus;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * PDF statement service using iText 7. (T027)
 */
@Service
public class PdfStatementService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("MMMM yyyy");

    /**
     * Builds a professional bank statement PDF for the given account and transaction list. (US-11)
     */
    public byte[] buildStatementPdf(long accountId,
                                     String accountNumber,
                                     String accountStatus,
                                     String customerName,
                                     YearMonth yearMonth,
                                     boolean isMonthToDate,
                                     BigDecimal openingBalance,
                                     BigDecimal closingBalance,
                                     BigDecimal totalIn,
                                     BigDecimal totalOut,
                                     List<Transaction> transactions) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (PdfWriter writer = new PdfWriter(baos);
             PdfDocument pdf = new PdfDocument(writer);
             Document doc = new Document(pdf)) {

            String periodLabel = yearMonth.format(MONTH_FMT)
                    + (isMonthToDate ? " (Month-to-Date)" : "");
            String generatedDate = LocalDate.now().format(DATE_FMT);
            String fullName = (customerName != null && !customerName.isBlank()) ? customerName : "N/A";

            // ---- Bank header ----
            doc.add(new Paragraph("VOLTIO Digital Bank")
                    .setFontSize(22).setBold().setTextAlignment(TextAlignment.CENTER));
            doc.add(new Paragraph("Account Statement")
                    .setFontSize(14).setTextAlignment(TextAlignment.CENTER)
                    .setFontColor(ColorConstants.DARK_GRAY));
            doc.add(new Paragraph(" "));

            // ---- Customer details section ----
            doc.add(new Paragraph("Customer Details").setFontSize(14).setBold());
            doc.add(buildDetailLine("Customer Name:", fullName));
            doc.add(buildDetailLine("Account Number:", accountNumber != null ? accountNumber : "N/A"));
            doc.add(buildDetailLine("Account Status:", accountStatus));
            doc.add(buildDetailLine("Statement Period:", periodLabel));
            doc.add(buildDetailLine("Generated Date:", generatedDate));
            doc.add(new Paragraph(" "));

            // ---- Summary section ----
            doc.add(new Paragraph("Account Summary").setFontSize(13).setBold());
            float[] summaryWidths = {200f, 200f};
            Table summary = new Table(UnitValue.createPointArray(summaryWidths));
            summary.addCell(summaryLabelCell("Opening Balance"));
            summary.addCell(summaryValueCell(formatAmount(openingBalance)));
            summary.addCell(summaryLabelCell("Closing Balance"));
            summary.addCell(summaryValueCell(formatAmount(closingBalance)));
            summary.addCell(summaryLabelCell("Total Money In"));
            summary.addCell(summaryValueCell(formatAmount(totalIn)));
            summary.addCell(summaryLabelCell("Total Money Out"));
            summary.addCell(summaryValueCell(formatAmount(totalOut)));
            doc.add(summary);
            doc.add(new Paragraph(" "));

            // ---- Transaction table ----
            doc.add(new Paragraph("Transaction History").setFontSize(13).setBold());
            float[] colWidths = {70f, 60f, 60f, 60f, 230f, 70f};
            Table table = new Table(UnitValue.createPointArray(colWidths));
            table.setWidth(UnitValue.createPercentValue(100));

            table.addHeaderCell(headerCell("Date"));
            table.addHeaderCell(headerCell("Amount"));
            table.addHeaderCell(headerCell("Type"));
            table.addHeaderCell(headerCell("Status"));
            table.addHeaderCell(headerCell("Description"));
            table.addHeaderCell(headerCell("Result"));

            if (transactions.isEmpty()) {
                table.addCell(new Cell(1, 6)
                        .add(new Paragraph("No transactions in the selected period.")
                                .setTextAlignment(TextAlignment.CENTER)));
            } else {
                for (Transaction t : transactions) {
                    boolean failed = t.getStatus() == TransactionStatus.FAILED;
                    table.addCell(dataCell(t.getTimestamp().atZone(ZoneOffset.UTC).format(DATE_FMT), failed));
                    table.addCell(dataCell(t.getAmount().toPlainString(), failed));
                    table.addCell(dataCell(t.getDirection().name(), failed));
                    table.addCell(dataCell(t.getStatus().name(), failed));
                    table.addCell(dataCell(t.getDescription() != null ? t.getDescription() : "", failed));
                    table.addCell(failedBadgeCell(failed));
                }
            }
            doc.add(table);

        } catch (Exception e) {
            throw new RuntimeException("Failed to generate PDF statement", e);
        }
        return baos.toByteArray();
    }

    /**
     * Builds an in-memory PDF statement for transaction history export (US-08).
     * @return PDF as byte array
     */
    public byte[] buildPdf(long accountId, LocalDate startDate, LocalDate endDate,
                            List<Transaction> transactions) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (PdfWriter writer = new PdfWriter(baos);
             PdfDocument pdf = new PdfDocument(writer);
             Document doc = new Document(pdf)) {

            String maskedAccountId = "****" + (accountId % 10000);
            doc.add(new Paragraph("Statement of Activity")
                    .setFontSize(18).setBold());
            doc.add(new Paragraph("Account: " + maskedAccountId)
                    .setFontSize(12));
            doc.add(new Paragraph("Period: " + startDate.format(DATE_FMT) + " to " + endDate.format(DATE_FMT))
                    .setFontSize(12));
            doc.add(new Paragraph("Generated: " + LocalDate.now().format(DATE_FMT))
                    .setFontSize(10));
            doc.add(new Paragraph(" "));

            float[] colWidths = {50f, 90f, 70f, 60f, 130f, 170f};
            Table table = new Table(UnitValue.createPercentArray(colWidths));
            table.setWidth(UnitValue.createPercentValue(100));

            table.addHeaderCell(new Cell().add(new Paragraph("ID").setBold()));
            table.addHeaderCell(new Cell().add(new Paragraph("Amount").setBold()));
            table.addHeaderCell(new Cell().add(new Paragraph("Type").setBold()));
            table.addHeaderCell(new Cell().add(new Paragraph("Status").setBold()));
            table.addHeaderCell(new Cell().add(new Paragraph("Timestamp").setBold()));
            table.addHeaderCell(new Cell().add(new Paragraph("Description").setBold()));

            for (Transaction t : transactions) {
                table.addCell(new Cell().add(new Paragraph(String.valueOf(t.getTransactionId()))));
                table.addCell(new Cell().add(new Paragraph(t.getAmount().toPlainString())));
                table.addCell(new Cell().add(new Paragraph(t.getDirection().name())));
                table.addCell(new Cell().add(new Paragraph(t.getStatus().name())));
                table.addCell(new Cell().add(new Paragraph(t.getTimestamp().atZone(ZoneOffset.UTC).format(DATE_FMT))));
                table.addCell(new Cell().add(new Paragraph(t.getDescription() != null ? t.getDescription() : "")));
            }

            if (transactions.isEmpty()) {
                table.addCell(new Cell(1, 6).add(new Paragraph("No transactions in the selected period.")));
            }

            doc.add(table);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate PDF statement", e);
        }
        return baos.toByteArray();
    }

    // ---- helpers ----

    private Paragraph buildDetailLine(String label, String value) {
        return new Paragraph()
                .add(new com.itextpdf.layout.element.Text(label + " ").setBold())
                .add(new com.itextpdf.layout.element.Text(value))
                .setFontSize(10);
    }

    private Cell summaryLabelCell(String text) {
        return new Cell().add(new Paragraph(text).setBold().setFontSize(11))
                .setBorder(new com.itextpdf.layout.borders.SolidBorder(0.5f));
    }

    private Cell summaryValueCell(String text) {
        return new Cell().add(new Paragraph(text).setFontSize(11).setTextAlignment(TextAlignment.RIGHT))
                .setBorder(new com.itextpdf.layout.borders.SolidBorder(0.5f));
    }

    private Cell headerCell(String text) {
        return new Cell().add(new Paragraph(text).setBold().setFontSize(10))
                .setBackgroundColor(ColorConstants.LIGHT_GRAY);
    }

    private Cell dataCell(String text, boolean failed) {
        Cell cell = new Cell().add(new Paragraph(text).setFontSize(9));
        if (failed) cell.setFontColor(ColorConstants.RED);
        return cell;
    }

    private Cell failedBadgeCell(boolean failed) {
        String label = failed ? "FAILED" : "";
        Cell cell = new Cell().add(new Paragraph(label).setFontSize(9).setBold());
        if (failed) cell.setFontColor(ColorConstants.RED);
        return cell;
    }

    private String formatAmount(BigDecimal amount) {
        return amount != null ? amount.setScale(2).toPlainString() : "0.00";
    }
}
