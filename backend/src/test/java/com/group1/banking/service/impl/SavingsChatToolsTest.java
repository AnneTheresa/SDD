package com.group1.banking.service.impl;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.vectorstore.VectorStore;

import com.group1.banking.dto.SavingsGoalResponse;
import com.group1.banking.dto.chat.AccountSummary;
import com.group1.banking.dto.chat.SpendCategorySummary;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Verifies each {@code @Tool} method on {@link SavingsChatTools} records itself on the
 * shared {@link ToolSelectionTracker}, since chat_interaction_log.tools_used depends on
 * every tool the model actually invokes reporting itself - not just the ones that found data.
 */
class SavingsChatToolsTest {

    private static final Long CUSTOMER_ID = 42L;

    private SavingsChatContextService contextService;
    private VectorStore vectorStore;
    private SavingsChatCitationTracker citationTracker;
    private ToolSelectionTracker toolSelectionTracker;
    private SavingsChatTools tools;

    @BeforeEach
    void setUp() {
        contextService = mock(SavingsChatContextService.class);
        vectorStore = mock(VectorStore.class);
        citationTracker = new SavingsChatCitationTracker();
        toolSelectionTracker = new ToolSelectionTracker();
        tools = new SavingsChatTools(contextService, vectorStore, citationTracker, toolSelectionTracker);
    }

    private ToolContext toolContext() {
        return new ToolContext(Map.of("customerId", CUSTOMER_ID));
    }

    @Test
    void getAccountSummaries_recordsItself_evenWhenNoAccounts() {
        when(contextService.getAccountSummaries(CUSTOMER_ID)).thenReturn(List.of());

        tools.getAccountSummaries(toolContext());

        assertThat(toolSelectionTracker.drainToolsUsed()).containsExactly("getAccountSummaries");
    }

    @Test
    void getRecentSpendingByCategory_recordsItself_evenWhenDataInsufficient() {
        when(contextService.getSpendByCategory(CUSTOMER_ID, 30))
                .thenReturn(new SpendCategorySummary(Map.of(), 1, 30, false));

        tools.getRecentSpendingByCategory(null, toolContext());

        assertThat(toolSelectionTracker.drainToolsUsed()).containsExactly("getRecentSpendingByCategory");
    }

    @Test
    void getSavingsGoals_recordsItself_evenWhenNoGoals() {
        when(contextService.getSavingsGoals(CUSTOMER_ID)).thenReturn(List.<SavingsGoalResponse>of());

        tools.getSavingsGoals(toolContext());

        assertThat(toolSelectionTracker.drainToolsUsed()).containsExactly("getSavingsGoals");
    }

    @Test
    void searchSavingsKnowledgeBase_recordsItself_evenWhenSearchFails() {
        tools.searchSavingsKnowledgeBase("emergency fund");

        assertThat(toolSelectionTracker.drainToolsUsed()).containsExactly("searchSavingsKnowledgeBase");
    }

    @Test
    void getAccountSummaries_recordsItself_whenAccountsFound() {
        when(contextService.getAccountSummaries(CUSTOMER_ID)).thenReturn(List.of(
                new AccountSummary(1L, "CHECKING", "ACTIVE", new BigDecimal("300.00"))));

        String reply = tools.getAccountSummaries(toolContext());

        assertThat(reply).contains("Account ID 1");
        assertThat(toolSelectionTracker.drainToolsUsed()).containsExactly("getAccountSummaries");
    }
}
