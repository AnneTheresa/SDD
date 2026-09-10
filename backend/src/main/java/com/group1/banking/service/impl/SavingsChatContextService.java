package com.group1.banking.service.impl;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.group1.banking.dto.RiskScoreResponse;
import com.group1.banking.dto.SavingsGoalResponse;
import com.group1.banking.dto.chat.AccountSummary;
import com.group1.banking.dto.chat.SpendCategorySummary;
import com.group1.banking.entity.Account;
import com.group1.banking.entity.AccountStatus;
import com.group1.banking.entity.Transaction;
import com.group1.banking.entity.TransactionDirection;
import com.group1.banking.entity.TransactionStatus;
import com.group1.banking.enums.RiskDirective;
import com.group1.banking.enums.RiskScoreStatus;
import com.group1.banking.repository.AccountRepository;
import com.group1.banking.repository.TransactionQueryRepository;
import com.group1.banking.service.SavingsGoalService;

/**
 * Read-only data access used by {@link SavingsChatTools}: each method here
 * backs one
 * agent tool, so they're deliberately small and focused rather than one big
 * "build everything" call. Pulls from the same repositories/services the REST
 * endpoints use, so figures shown in chat match what the customer sees
 * elsewhere.
 */
@Service
public class SavingsChatContextService {

    private final AccountRepository accountRepository;
    private final TransactionQueryRepository transactionQueryRepository;
    private final SavingsGoalService savingsGoalService;
    private final RiskScoreService riskScoreService;

    @Value("${app.chatbot.transactions.min-for-personalization:3}")
    private int minTransactionsForPersonalization;

    public SavingsChatContextService(AccountRepository accountRepository,
            TransactionQueryRepository transactionQueryRepository,
            SavingsGoalService savingsGoalService,
            RiskScoreService riskScoreService) {
        this.accountRepository = accountRepository;
        this.transactionQueryRepository = transactionQueryRepository;
        this.savingsGoalService = savingsGoalService;
        this.riskScoreService = riskScoreService;
    }

    public List<Account> getOpenAccounts(Long customerId) {
        return accountRepository
                .findAllByCustomerCustomerIdAndDeletedAtIsNullAndStatusNot(customerId, AccountStatus.CLOSED);
    }

    public List<AccountSummary> getAccountSummaries(Long customerId) {
        return getOpenAccounts(customerId).stream()
                .map(a -> new AccountSummary(a.getAccountId(), a.getAccountType().name(),
                        a.getStatus().name(), a.getBalance()))
                .toList();
    }

    public SpendCategorySummary getSpendByCategory(Long customerId, int lookbackDays) {
        Instant end = Instant.now();
        Instant start = end.minus(lookbackDays, ChronoUnit.DAYS);

        Map<String, BigDecimal> categorySpend = new LinkedHashMap<>();
        int transactionCount = 0;

        for (Account account : getOpenAccounts(customerId)) {
            List<Transaction> transactions = transactionQueryRepository
                    .findByAccount_AccountIdAndTimestampBetweenOrderByTimestampAsc(
                            account.getAccountId(), start, end);

            for (Transaction t : transactions) {
                boolean isSpend = t.getStatus() == TransactionStatus.SUCCESS
                        && (t.getDirection() == TransactionDirection.DEBIT
                                || t.getDirection() == TransactionDirection.TRANSFER);
                if (!isSpend) {
                    continue;
                }
                transactionCount++;
                String category = (t.getCategory() == null || t.getCategory().isBlank())
                        ? "Uncategorised"
                        : t.getCategory();
                categorySpend.merge(category, t.getAmount(), BigDecimal::add);
            }
        }

        boolean sufficientData = transactionCount >= minTransactionsForPersonalization;
        return new SpendCategorySummary(categorySpend, transactionCount, lookbackDays, sufficientData);
    }

    public List<SavingsGoalResponse> getSavingsGoals(Long customerId) {
        return savingsGoalService.getAllGoalsForCustomer(customerId);
    }

    public boolean hasFrozenAccount(Long customerId) {
        return getOpenAccounts(customerId).stream().anyMatch(a -> a.getStatus() == AccountStatus.FROZEN);
    }

    public String getRiskGuidanceDirective(Long customerId) {
        RiskScoreResponse res = riskScoreService.calculateRiskScoreWithoutSaving(customerId);
        if (res.getCalculateStatus() == RiskScoreStatus.INSUFFICIENT_DATA || res.getLevel() == null) {
            return null;
        }
        return RiskDirective.get(res.getLevel()).directive();
    }

}
