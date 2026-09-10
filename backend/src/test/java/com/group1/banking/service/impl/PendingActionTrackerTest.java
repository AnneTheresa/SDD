package com.group1.banking.service.impl;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class PendingActionTrackerTest {

    @Test
    void drainProposal_shouldReturnEmpty_whenNothingRecorded() {
        PendingActionTracker tracker = new PendingActionTracker();
        assertThat(tracker.drainProposal()).isEmpty();
    }

    @Test
    void drainProposal_shouldReturnRecordedProposalOnce() {
        PendingActionTracker tracker = new PendingActionTracker();
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(5);

        tracker.recordProposal("tok-1", "TRANSFER", "Transfer $50.00", expiresAt);

        Optional<com.group1.banking.dto.chat.PendingConfirmationView> first = tracker.drainProposal();
        assertThat(first).isPresent();
        assertThat(first.get().getToken()).isEqualTo("tok-1");
        assertThat(first.get().getActionType()).isEqualTo("TRANSFER");
        assertThat(first.get().getSummary()).isEqualTo("Transfer $50.00");
        assertThat(first.get().getExpiresAt()).isEqualTo(expiresAt);

        assertThat(tracker.drainProposal()).isEmpty();
    }

    @Test
    void reset_shouldClearAnyRecordedProposal() {
        PendingActionTracker tracker = new PendingActionTracker();
        tracker.recordProposal("tok-2", "TRANSFER", "Transfer $10.00", LocalDateTime.now().plusMinutes(5));

        tracker.reset();

        assertThat(tracker.drainProposal()).isEmpty();
    }
}
