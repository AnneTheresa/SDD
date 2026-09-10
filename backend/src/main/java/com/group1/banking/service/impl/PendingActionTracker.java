package com.group1.banking.service.impl;

import com.group1.banking.dto.chat.PendingConfirmationView;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Carries "a mutating tool proposed an action this turn" out of the agentic
 * tool-calling loop so SavingsInsightChatService can attach it to the chat response
 * as structured data. Same reset-per-turn, drain-once shape as
 * {@link SavingsChatCitationTracker} - see that class for the concurrency caveat
 * this shares (a per-turn singleton is safe only because turns aren't processed
 * concurrently against the same bean instance today).
 */
@Component
public class PendingActionTracker {

    private String token;
    private String actionType;
    private String summary;
    private LocalDateTime expiresAt;

    public void reset() {
        token = null;
        actionType = null;
        summary = null;
        expiresAt = null;
    }

    public void recordProposal(String token, String actionType, String summary, LocalDateTime expiresAt) {
        this.token = token;
        this.actionType = actionType;
        this.summary = summary;
        this.expiresAt = expiresAt;
    }

    public Optional<PendingConfirmationView> drainProposal() {
        if (token == null) {
            return Optional.empty();
        }
        PendingConfirmationView view = new PendingConfirmationView(token, actionType, summary, expiresAt);
        reset();
        return Optional.of(view);
    }
}
