package com.group1.banking.service.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class ToolSelectionTrackerTest {

    @Test
    void drainToolsUsed_shouldReturnEmpty_whenNothingRecorded() {
        ToolSelectionTracker tracker = new ToolSelectionTracker();

        assertThat(tracker.drainToolsUsed()).isEmpty();
    }

    @Test
    void recordTool_shouldCaptureOrder_andDrainOnce() {
        ToolSelectionTracker tracker = new ToolSelectionTracker();

        tracker.recordTool("getGicRates");
        tracker.recordTool("searchKnowledgeBase");

        assertThat(tracker.drainToolsUsed()).containsExactly("getGicRates", "searchKnowledgeBase");
        assertThat(tracker.drainToolsUsed()).isEmpty();
    }

    @Test
    void reset_shouldClearRecordedTools() {
        ToolSelectionTracker tracker = new ToolSelectionTracker();

        tracker.recordTool("getGicRates");
        tracker.reset();

        assertThat(tracker.drainToolsUsed()).isEmpty();
    }
}
