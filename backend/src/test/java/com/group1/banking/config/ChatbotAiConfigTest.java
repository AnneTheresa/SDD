package com.group1.banking.config;

import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;

import com.group1.banking.service.impl.SavingsChatTools;
import com.group1.banking.service.impl.ToolSelectionTracker;
import com.group1.banking.service.impl.ToolTrackingCallback;
import com.group1.banking.service.impl.TransferChatTool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Verifies which tools {@link ChatbotAiConfig} wires into the savings insight
 * ChatClient's default tools set, depending on whether an MCP
 * {@link ToolCallbackProvider} bean is available. Tests the extracted
 * {@code resolveDefaultTools} helper directly rather than reflecting into
 * Spring AI's ChatClient internals, since the client itself does not expose
 * its configured tools for inspection.
 */
class ChatbotAiConfigTest {

    private final SavingsChatTools savingsChatTools = mock(SavingsChatTools.class);
    private final TransferChatTool transferChatTool = mock(TransferChatTool.class);

    @Test
    void resolveDefaultTools_shouldIncludeMcpProvider_whenAvailable() {
        ToolCallbackProvider mcpProvider = mock(ToolCallbackProvider.class);

        Object[] tools = ChatbotAiConfig.resolveDefaultTools(savingsChatTools, transferChatTool, mcpProvider);

        assertThat(tools).containsExactly(savingsChatTools, transferChatTool, mcpProvider);
    }

    @Test
    void resolveDefaultTools_shouldOmitMcpProvider_whenUnavailable() {
        Object[] tools = ChatbotAiConfig.resolveDefaultTools(savingsChatTools, transferChatTool, null);

        assertThat(tools).containsExactly(savingsChatTools, transferChatTool);
    }

    @Test
    void withToolTracking_shouldWrapEveryCallbackFromTheMcpProvider() {
        ToolCallback rawCallback = mock(ToolCallback.class);
        ToolCallbackProvider rawProvider = mock(ToolCallbackProvider.class);
        when(rawProvider.getToolCallbacks()).thenReturn(new ToolCallback[] { rawCallback });
        ToolSelectionTracker toolSelectionTracker = new ToolSelectionTracker();

        ToolCallbackProvider tracked = ChatbotAiConfig.withToolTracking(rawProvider, toolSelectionTracker);

        assertThat(tracked.getToolCallbacks()).hasSize(1);
        assertThat(tracked.getToolCallbacks()[0]).isInstanceOf(ToolTrackingCallback.class);
    }
}
