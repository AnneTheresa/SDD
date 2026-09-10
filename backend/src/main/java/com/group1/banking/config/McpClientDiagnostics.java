package com.group1.banking.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Confirms the MCP (Model Context Protocol) client is wired up at startup and reports
 * its status without ever failing that startup.
 *
 * By default, the chatbot's Agent connects to a rates MCP server (see
 * spring.ai.mcp.client.sse.connections.rates-server in application.properties) that
 * serves the GIC rate lookup tool used alongside the savings knowledge base for GIC
 * inquiries. That connection is lazily initialized
 * (spring.ai.mcp.client.initialized=false), so it is only actually attempted on first
 * use, not during startup - this listener's own call to
 * {@code provider.getToolCallbacks()} may be that first use, and is wrapped
 * accordingly: if the rates server is unreachable, this logs a warning and the
 * backend still starts normally, and chat requests remain available with the
 * MCP-backed tool simply absent from that turn (Spring AI surfaces the failure to the
 * model as a tool error rather than failing the request - see
 * spring.ai.tools.throw-exception-on-error).
 *
 * When the opt-in "mcptest" profile is active (see application-mcptest.properties)
 * and the standalone MCP test module is running with an overridden endpoint, a
 * "ping" tool may also be available. In that case this calls it and logs the real
 * response, proving a full MCP round trip (initialize -> list tools -> call tool)
 * beyond just tool discovery. That call only ever happens if a tool literally named
 * "ping" is present, so it's a no-op otherwise.
 *
 * Uses {@link ObjectProvider} rather than a required constructor dependency so that
 * if no MCP-related bean is ever produced (e.g. the starter is removed, or
 * autoconfiguration changes across a Spring AI version bump), this component
 * degrades to a no-op log line instead of failing application startup.
 */
@Component
public class McpClientDiagnostics {

    private static final Logger log = LoggerFactory.getLogger(McpClientDiagnostics.class);

    private static final String PING_TOOL_NAME = "ping";

    private final ObjectProvider<ToolCallbackProvider> toolCallbackProvider;

    public McpClientDiagnostics(ObjectProvider<ToolCallbackProvider> toolCallbackProvider) {
        this.toolCallbackProvider = toolCallbackProvider;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void logMcpStatus() {
        ToolCallbackProvider provider = toolCallbackProvider.getIfAvailable();
        if (provider == null) {
            log.info("MCP client: no ToolCallbackProvider bean found (is spring-ai-starter-mcp-client "
                    + "on the classpath?). No MCP tools are available.");
            return;
        }

        ToolCallback[] callbacks;
        try {
            callbacks = provider.getToolCallbacks();
        } catch (Exception ex) {
            log.warn("MCP client: could not reach the configured MCP server(s) at startup. "
                    + "Backend startup continues normally - MCP-backed tools (e.g. GIC rates) will "
                    + "simply be unavailable to the chatbot until the server is reachable.", ex);
            return;
        }

        log.info("MCP client wired up: {} tool callback(s) available from the configured MCP server(s).",
                callbacks.length);

        for (ToolCallback callback : callbacks) {
            if (PING_TOOL_NAME.equals(callback.getToolDefinition().name())) {
                callPingForRoundTripCheck(callback);
            }
        }
    }

    private void callPingForRoundTripCheck(ToolCallback pingCallback) {
        try {
            String result = pingCallback.call("{}");
            log.info("MCP client round-trip check: called the '{}' tool on the connected test server "
                    + "and got back: {}", PING_TOOL_NAME, result);
        } catch (Exception ex) {
            log.warn("MCP client round-trip check: found the '{}' tool but calling it failed. "
                    + "Tool discovery worked; invocation did not.", PING_TOOL_NAME, ex);
        }
    }
}
