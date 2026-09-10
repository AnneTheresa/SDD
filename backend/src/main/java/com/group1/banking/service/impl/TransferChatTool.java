package com.group1.banking.service.impl;

import com.group1.banking.dto.chat.AccountSummary;
import com.group1.banking.dto.customer.TransferRequest;
import com.group1.banking.entity.PendingAgentActionEntity;
import com.group1.banking.entity.PendingAgentActionType;
import com.group1.banking.service.ConfirmationGateService;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * The only mutating tool the Savings Insight chatbot agent can call. It never moves
 * money itself - it validates the request and hands off to {@link ConfirmationGateService}
 * to create a pending proposal, which only the separate
 * {@code POST /api/chat/confirmations/{token}} endpoint can later execute. This is
 * what makes AC4 ("never autonomously completes a financial action") a structural
 * property rather than a prompted behaviour.
 */
@Component
public class TransferChatTool {

    private static final String CUSTOMER_ID_KEY = "customerId";
    private static final String ACTOR_ROLE_KEY = "actorRole";
    private static final BigDecimal MIN_AMOUNT = new BigDecimal("0.01");

    private final SavingsChatContextService contextService;
    private final ConfirmationGateService confirmationGateService;
    private final PendingActionTracker pendingActionTracker;
    private final ToolSelectionTracker toolSelectionTracker;

    public TransferChatTool(SavingsChatContextService contextService,
                             ConfirmationGateService confirmationGateService,
                             PendingActionTracker pendingActionTracker,
                             ToolSelectionTracker toolSelectionTracker) {
        this.contextService = contextService;
        this.confirmationGateService = confirmationGateService;
        this.pendingActionTracker = pendingActionTracker;
        this.toolSelectionTracker = toolSelectionTracker;
    }

    @Tool(description = "Propose a transfer of money between two of the customer's own accounts. "
            + "This does NOT move any money - it only prepares the transfer for the customer to "
            + "explicitly confirm in the chat window. After calling this, tell the customer the "
            + "transfer is awaiting their confirmation - never say it is complete or that the money "
            + "has moved.")
    public String proposeTransfer(
            @ToolParam(description = "The account ID to transfer money FROM") Long fromAccountId,
            @ToolParam(description = "The account ID to transfer money TO") Long toAccountId,
            @ToolParam(description = "The amount to transfer, e.g. 200.00") BigDecimal amount,
            @ToolParam(description = "A short description/memo for the transfer", required = false) String description,
            ToolContext toolContext) {

        toolSelectionTracker.recordTool("proposeTransfer");
        Long customerId = requireCustomerId(toolContext);
        String actorRole = requireActorRole(toolContext);

        if (fromAccountId != null && fromAccountId.equals(toAccountId)) {
            return "I can't propose a transfer to the same account.";
        }
        if (amount == null || amount.compareTo(MIN_AMOUNT) < 0) {
            return "I can't propose a transfer for that amount - it must be at least $0.01.";
        }

        List<AccountSummary> accounts = contextService.getAccountSummaries(customerId);
        Optional<AccountSummary> from = accounts.stream().filter(a -> a.accountId().equals(fromAccountId)).findFirst();
        Optional<AccountSummary> to = accounts.stream().filter(a -> a.accountId().equals(toAccountId)).findFirst();

        if (from.isEmpty() || to.isEmpty()) {
            return "I can't propose that transfer - one or both of those accounts aren't available to you.";
        }
        if (from.get().balance().compareTo(amount) < 0) {
            return "I can't propose that transfer - the source account's balance ($" + from.get().balance()
                    + ") is less than the requested amount.";
        }

        TransferRequest request = new TransferRequest(fromAccountId, toAccountId, amount, description);
        String summary = String.format("Transfer $%s from your %s account (#%d) to your %s account (#%d).",
                amount, from.get().accountType(), fromAccountId, to.get().accountType(), toAccountId);

        PendingAgentActionEntity proposal = confirmationGateService.propose(
                customerId, actorRole, PendingAgentActionType.TRANSFER, request, summary);

        pendingActionTracker.recordProposal(proposal.getToken(), PendingAgentActionType.TRANSFER.name(),
                proposal.getHumanSummary(), proposal.getExpiresAt());

        return summary + " This has NOT happened yet - it is awaiting the customer's explicit confirmation "
                + "in the chat window. Confirmation code: " + proposal.getToken();
    }

    private Long requireCustomerId(ToolContext toolContext) {
        Object customerId = toolContext.getContext().get(CUSTOMER_ID_KEY);
        if (!(customerId instanceof Long id)) {
            throw new IllegalStateException("Transfer tool invoked without a customerId in ToolContext");
        }
        return id;
    }

    private String requireActorRole(ToolContext toolContext) {
        Object actorRole = toolContext.getContext().get(ACTOR_ROLE_KEY);
        if (!(actorRole instanceof String role)) {
            throw new IllegalStateException("Transfer tool invoked without an actorRole in ToolContext");
        }
        return role;
    }
}
