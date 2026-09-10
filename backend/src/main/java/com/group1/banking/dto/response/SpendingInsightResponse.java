package com.group1.banking.dto.response;

import java.math.BigDecimal;
import java.util.List;

/**
 * Spending insight response DTO. (T094)
 */
public class SpendingInsightResponse {

    private Long accountId;
    private PeriodInfo period;
    private BigDecimal totalDebitSpend;
    private int transactionCount;
    private boolean hasUncategorised;
    private boolean hasExcludedDisputes;
    private boolean dataFresh;
    private List<CategoryBreakdownItem> categoryBreakdown;
    private List<TransactionItemResponse> topTransactions;
    private List<MonthTrendItem> sixMonthTrend;

    public static class PeriodInfo {
        private int year;
        private int month;
        private boolean isComplete;

        public int getYear() { return year; }
        public void setYear(int year) { this.year = year; }
        public int getMonth() { return month; }
        public void setMonth(int month) { this.month = month; }
        public boolean isComplete() { return isComplete; }
        public void setComplete(boolean complete) { isComplete = complete; }
    }

    public static class CategoryBreakdownItem {
        private String category;
        private BigDecimal totalAmount;
        private BigDecimal percentage;
        private int transactionCount;

        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        public BigDecimal getTotalAmount() { return totalAmount; }
        public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
        public BigDecimal getPercentage() { return percentage; }
        public void setPercentage(BigDecimal percentage) { this.percentage = percentage; }
        public int getTransactionCount() { return transactionCount; }
        public void setTransactionCount(int transactionCount) { this.transactionCount = transactionCount; }
    }

    public static class MonthTrendItem {
        private int year;
        private int month;
        private BigDecimal totalSpend;
        private boolean isComplete;
        private boolean accountExisted;

        public int getYear() { return year; }
        public void setYear(int year) { this.year = year; }
        public int getMonth() { return month; }
        public void setMonth(int month) { this.month = month; }
        public BigDecimal getTotalSpend() { return totalSpend; }
        public void setTotalSpend(BigDecimal totalSpend) { this.totalSpend = totalSpend; }
        public boolean isComplete() { return isComplete; }
        public void setComplete(boolean complete) { isComplete = complete; }
        public boolean isAccountExisted() { return accountExisted; }
        public void setAccountExisted(boolean accountExisted) { this.accountExisted = accountExisted; }
    }

    public Long getAccountId() { return accountId; }
    public void setAccountId(Long accountId) { this.accountId = accountId; }
    public PeriodInfo getPeriod() { return period; }
    public void setPeriod(PeriodInfo period) { this.period = period; }
    public BigDecimal getTotalDebitSpend() { return totalDebitSpend; }
    public void setTotalDebitSpend(BigDecimal totalDebitSpend) { this.totalDebitSpend = totalDebitSpend; }
    public int getTransactionCount() { return transactionCount; }
    public void setTransactionCount(int transactionCount) { this.transactionCount = transactionCount; }
    public boolean isHasUncategorised() { return hasUncategorised; }
    public void setHasUncategorised(boolean hasUncategorised) { this.hasUncategorised = hasUncategorised; }
    public boolean isHasExcludedDisputes() { return hasExcludedDisputes; }
    public void setHasExcludedDisputes(boolean hasExcludedDisputes) { this.hasExcludedDisputes = hasExcludedDisputes; }
    public boolean isDataFresh() { return dataFresh; }
    public void setDataFresh(boolean dataFresh) { this.dataFresh = dataFresh; }
    public List<CategoryBreakdownItem> getCategoryBreakdown() { return categoryBreakdown; }
    public void setCategoryBreakdown(List<CategoryBreakdownItem> categoryBreakdown) { this.categoryBreakdown = categoryBreakdown; }
    public List<TransactionItemResponse> getTopTransactions() { return topTransactions; }
    public void setTopTransactions(List<TransactionItemResponse> topTransactions) { this.topTransactions = topTransactions; }
    public List<MonthTrendItem> getSixMonthTrend() { return sixMonthTrend; }
    public void setSixMonthTrend(List<MonthTrendItem> sixMonthTrend) { this.sixMonthTrend = sixMonthTrend; }
}
