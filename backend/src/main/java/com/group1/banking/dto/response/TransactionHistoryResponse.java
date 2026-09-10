package com.group1.banking.dto.response;

import java.time.Instant;
import java.util.List;

/**
 * Transaction history response. (T024)
 */
public class TransactionHistoryResponse {
    private Long accountId;
    private Instant startDate;
    private Instant endDate;
    private int transactionCount;
    private List<TransactionItemResponse> transactions;

    public TransactionHistoryResponse() {}

    public Long getAccountId() { return accountId; }
    public void setAccountId(Long accountId) { this.accountId = accountId; }
    public Instant getStartDate() { return startDate; }
    public void setStartDate(Instant startDate) { this.startDate = startDate; }
    public Instant getEndDate() { return endDate; }
    public void setEndDate(Instant endDate) { this.endDate = endDate; }
    public int getTransactionCount() { return transactionCount; }
    public void setTransactionCount(int transactionCount) { this.transactionCount = transactionCount; }
    public List<TransactionItemResponse> getTransactions() { return transactions; }
    public void setTransactions(List<TransactionItemResponse> transactions) { this.transactions = transactions; }
}
