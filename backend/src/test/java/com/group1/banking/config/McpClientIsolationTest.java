package com.group1.banking.config;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema.Implementation;
import io.modelcontextprotocol.spec.McpSchema.ListToolsResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class McpClientIsolationTest {

    @Test
    void unavailableRatesServer_doesNotInterruptDeveloperToolDiscovery() {
        McpSyncClient ratesClient = client("voltio-agent - rates-server");
        McpSyncClient developerClient = client("voltio-agent - developer-server");
        when(ratesClient.listTools()).thenThrow(new IllegalStateException("MCP server unavailable"));
        when(developerClient.listTools()).thenReturn(new ListToolsResult(List.of(), null));

        ToolCallbackProvider developerProvider = ChatbotAiConfig.toolCallbackProviderForConnection(
                List.of(ratesClient, developerClient), "developer-server");

        assertThat(developerProvider).isNotNull();
        assertThat(developerProvider.getToolCallbacks()).isEmpty();
        verify(developerClient).listTools();
        verify(ratesClient, never()).listTools();

        ToolCallbackProvider ratesProvider = ChatbotAiConfig.toolCallbackProviderForConnection(
                List.of(ratesClient, developerClient), "rates-server");
        assertThatThrownBy(ratesProvider::getToolCallbacks)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("MCP server unavailable");
        verify(ratesClient).listTools();
        verify(developerClient).listTools();
    }

    @Test
    void springInjectsConfiguredClientsIntoDeveloperProviderFactory() {
        McpSyncClient ratesClient = client("voltio-agent - rates-server");
        McpSyncClient developerClient = client("voltio-agent - developer-server");
        when(ratesClient.listTools()).thenThrow(new IllegalStateException("MCP server unavailable"));
        when(developerClient.listTools()).thenReturn(new ListToolsResult(List.of(), null));

        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.getBeanFactory().registerSingleton("ratesMcpClient", ratesClient);
            context.getBeanFactory().registerSingleton("developerMcpClient", developerClient);
            context.register(ProviderConfiguration.class);
            context.refresh();

            ToolCallbackProvider provider = context.getBean(
                    "developerMcpToolCallbackProvider", ToolCallbackProvider.class);
            assertThat(provider.getToolCallbacks()).isEmpty();
        }

        verify(developerClient).listTools();
        verify(ratesClient, never()).listTools();
    }

    private McpSyncClient client(String clientName) {
        McpSyncClient client = mock(McpSyncClient.class);
        when(client.getClientInfo()).thenReturn(new Implementation(clientName, "0.1.0"));
        return client;
    }

    @Configuration(proxyBeanMethods = false)
    static class ProviderConfiguration {

        @Bean("developerMcpToolCallbackProvider")
        ToolCallbackProvider developerMcpToolCallbackProvider(List<McpSyncClient> clients) {
            return new ChatbotAiConfig().developerMcpToolCallbackProvider(clients);
        }
    }
}
