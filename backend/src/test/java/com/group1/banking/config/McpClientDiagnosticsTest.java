package com.group1.banking.config;

import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.beans.factory.ObjectProvider;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Verifies backend startup never fails because the rates MCP server is unreachable
 * (US3): {@link McpClientDiagnostics} is an {@code ApplicationReadyEvent} listener, so
 * any exception it lets escape would fail the whole application context refresh.
 */
class McpClientDiagnosticsTest {

    @SuppressWarnings("unchecked")
    private ObjectProvider<ToolCallbackProvider> objectProvider() {
        return mock(ObjectProvider.class);
    }

    @Test
    void logMcpStatus_doesNotThrow_whenMcpServerIsUnreachable() {
        ToolCallbackProvider provider = mock(ToolCallbackProvider.class);
        when(provider.getToolCallbacks()).thenThrow(new RuntimeException("connection refused"));
        ObjectProvider<ToolCallbackProvider> objectProvider = objectProvider();
        when(objectProvider.getIfAvailable()).thenReturn(provider);
        McpClientDiagnostics diagnostics = new McpClientDiagnostics(objectProvider);

        assertThatCode(diagnostics::logMcpStatus).doesNotThrowAnyException();
    }

    @Test
    void logMcpStatus_doesNotThrow_whenNoProviderBeanExists() {
        ObjectProvider<ToolCallbackProvider> objectProvider = objectProvider();
        when(objectProvider.getIfAvailable()).thenReturn(null);
        McpClientDiagnostics diagnostics = new McpClientDiagnostics(objectProvider);

        assertThatCode(diagnostics::logMcpStatus).doesNotThrowAnyException();
    }

    @Test
    void logMcpStatus_doesNotThrow_whenServerIsReachableAndHasCallbacks() {
        ToolDefinition definition = mock(ToolDefinition.class);
        when(definition.name()).thenReturn("getGicRates");
        ToolCallback callback = mock(ToolCallback.class);
        when(callback.getToolDefinition()).thenReturn(definition);
        ToolCallbackProvider provider = mock(ToolCallbackProvider.class);
        when(provider.getToolCallbacks()).thenReturn(new ToolCallback[] { callback });
        ObjectProvider<ToolCallbackProvider> objectProvider = objectProvider();
        when(objectProvider.getIfAvailable()).thenReturn(provider);
        McpClientDiagnostics diagnostics = new McpClientDiagnostics(objectProvider);

        assertThatCode(diagnostics::logMcpStatus).doesNotThrowAnyException();
    }
}
