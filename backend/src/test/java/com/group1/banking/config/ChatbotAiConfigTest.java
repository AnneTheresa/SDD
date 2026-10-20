package com.group1.banking.config;

import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.definition.ToolDefinition;

import com.group1.banking.service.impl.SavingsChatTools;
import com.group1.banking.service.impl.DeveloperChatEvidenceTracker;
import com.group1.banking.service.impl.DeveloperEvidenceTrackingCallback;
import com.group1.banking.service.impl.ToolSelectionTracker;
import com.group1.banking.service.impl.ToolTrackingCallback;
import com.group1.banking.service.impl.TransferChatTool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
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

    @Test
    void developerCallbackEvidence_isTrackedIndependentlyFromCustomerCallbacks() {
        ToolCallback customerCallback = callback("savings_account_summaries");
        ToolCallback developerCallback = callback("developer_search_repository");
        when(developerCallback.call(anyString())).thenReturn(
                "SOURCE: type=github; path=src/App.jsx; url=https://github.com/example/repo/blob/main/src/App.jsx; "
                        + "revision=branch:main");
        DeveloperChatEvidenceTracker evidenceTracker = new DeveloperChatEvidenceTracker();
        evidenceTracker.beginTurn();

        ToolCallbackProvider customerProvider = () -> new ToolCallback[] { customerCallback };
        ToolCallbackProvider developerProvider = () -> new ToolCallback[] { developerCallback };

        ToolCallback[] customerTools = customerProvider.getToolCallbacks();
        ToolCallback[] developerTools = ChatbotAiConfig.onlyDeveloperTools(developerProvider, evidenceTracker)
                .getToolCallbacks();
        assertThat(customerTools).containsExactly(customerCallback);
        assertThat(developerTools).singleElement().isInstanceOf(DeveloperEvidenceTrackingCallback.class);
        assertThat(developerTools[0].call("{}")).contains("SOURCE: type=github");
        assertThat(evidenceTracker.snapshot().sources()).singleElement()
                .extracting(source -> source.path()).isEqualTo("src/App.jsx");
        evidenceTracker.clear();
    }

    private ToolCallback callback(String name) {
        ToolCallback callback = mock(ToolCallback.class);
        ToolDefinition definition = mock(ToolDefinition.class);
        when(definition.name()).thenReturn(name);
        when(callback.getToolDefinition()).thenReturn(definition);
        return callback;
    }

}
