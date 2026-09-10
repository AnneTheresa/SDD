package com.group1.banking.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ToolTrackingCallbackTest {

    private ToolCallback delegate;
    private ToolDefinition definition;
    private ToolSelectionTracker toolSelectionTracker;
    private ToolTrackingCallback callback;

    @BeforeEach
    void setUp() {
        delegate = mock(ToolCallback.class);
        definition = mock(ToolDefinition.class);
        when(definition.name()).thenReturn("getGicRates");
        when(delegate.getToolDefinition()).thenReturn(definition);
        toolSelectionTracker = new ToolSelectionTracker();
        callback = new ToolTrackingCallback(delegate, toolSelectionTracker);
    }

    @Test
    void call_singleArg_recordsToolNameAndDelegates() {
        when(delegate.call("{}")).thenReturn("SIX_MONTHS: 3.00%");

        String result = callback.call("{}");

        assertThat(result).isEqualTo("SIX_MONTHS: 3.00%");
        assertThat(toolSelectionTracker.drainToolsUsed()).containsExactly("getGicRates");
    }

    @Test
    void call_withToolContext_recordsToolNameAndDelegates() {
        ToolContext context = new ToolContext(Map.of("customerId", 42L));
        when(delegate.call("{}", context)).thenReturn("SIX_MONTHS: 3.00%");

        String result = callback.call("{}", context);

        assertThat(result).isEqualTo("SIX_MONTHS: 3.00%");
        assertThat(toolSelectionTracker.drainToolsUsed()).containsExactly("getGicRates");
    }

    @Test
    void getToolDefinition_delegatesUnchanged() {
        assertThat(callback.getToolDefinition()).isSameAs(definition);
    }
}
