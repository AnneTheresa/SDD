package com.group1.banking.service.impl;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Component;

/**
 * Records, per chat turn, which data sources the agent's tools actually pulled from
 * while answering (e.g. "Your recent transaction history", "Savings knowledge base:
 * ...md"). Since tool calls happen inside the model's reasoning loop rather than in
 * code we control directly, this is the mechanism for reconstructing the "basis" of
 * an answer and whether it used any real customer data (for the limited_data flag).
 *
 * Scoped per request via ThreadLocal: {@link SavingsInsightChatService} calls
 * {@link #reset()} before invoking the chat client (on the same thread, synchronously)
 * and {@link #drainCitations()} / {@link #usedPersonalData()} immediately after.
 */
@Component
public class SavingsChatCitationTracker {

    private static final ThreadLocal<Set<String>> CITATIONS = ThreadLocal.withInitial(LinkedHashSet::new);
    private static final ThreadLocal<Boolean> PERSONAL_DATA_USED = ThreadLocal.withInitial(() -> Boolean.FALSE);

    public void reset() {
        CITATIONS.remove();
        PERSONAL_DATA_USED.remove();
    }

    public void recordCitation(String citation) {
        CITATIONS.get().add(citation);
    }

    public void markPersonalDataUsed() {
        PERSONAL_DATA_USED.set(Boolean.TRUE);
    }

    public List<String> drainCitations() {
        List<String> result = new ArrayList<>(CITATIONS.get());
        CITATIONS.remove();
        return result;
    }

    public boolean drainUsedPersonalData() {
        boolean used = PERSONAL_DATA_USED.get();
        PERSONAL_DATA_USED.remove();
        return used;
    }
}
