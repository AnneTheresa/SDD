package com.group1.banking.dto.response;

import java.util.List;

/**
 * Recategorise transaction response DTO. (T096)
 */
public class RecategoriseResponse {
    private String transactionId;
    private String previousCategory;
    private String updatedCategory;
    private java.math.BigDecimal updatedTotalDebitSpend;
    private List<SpendingInsightResponse.CategoryBreakdownItem> updatedCategoryBreakdown;

    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
    public String getPreviousCategory() { return previousCategory; }
    public void setPreviousCategory(String previousCategory) { this.previousCategory = previousCategory; }
    public String getUpdatedCategory() { return updatedCategory; }
    public void setUpdatedCategory(String updatedCategory) { this.updatedCategory = updatedCategory; }
    public java.math.BigDecimal getUpdatedTotalDebitSpend() { return updatedTotalDebitSpend; }
    public void setUpdatedTotalDebitSpend(java.math.BigDecimal updatedTotalDebitSpend) { this.updatedTotalDebitSpend = updatedTotalDebitSpend; }
    public List<SpendingInsightResponse.CategoryBreakdownItem> getUpdatedCategoryBreakdown() { return updatedCategoryBreakdown; }
    public void setUpdatedCategoryBreakdown(List<SpendingInsightResponse.CategoryBreakdownItem> updatedCategoryBreakdown) { this.updatedCategoryBreakdown = updatedCategoryBreakdown; }
}
