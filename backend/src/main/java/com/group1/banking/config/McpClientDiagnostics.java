package com.group1.banking.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Confirms the MCP (Model Context Protocol) client is wired up at startup and reports
 * its status without ever failing that startup.
 *
 * Configured MCP connections are consumed independently by the customer and
 * developer chat clients. This diagnostic checks only the developer provider, so a
 * failure in an unrelated connection cannot prevent startup or developer tool
 * discovery.
 *
 * When the opt-in "mcptest" profile is active (see application-mcptest.properties)
 * and the standalone MCP test module is running with an overridden endpoint, a
 * "ping" tool may also be available. In that case this calls it and logs the real
 * response, proving a full MCP round trip (initialize -> list tools -> call tool)
 * beyond just tool discovery. That call only ever happens if a tool literally named
 * "ping" is present, so it's a no-op otherwise.
 *
 */
@Component
public class McpClientDiagnostics {

    private static final Logger log = LoggerFactory.getLogger(McpClientDiagnostics.class);

    private static final String PING_TOOL_NAME = "ping";

    private final ToolCallbackProvider toolCallbackProvider;

    public McpClientDiagnostics(
            @Qualifier("developerMcpToolCallbackProvider") ToolCallbackProvider toolCallbackProvider) {
        this.toolCallbackProvider = toolCallbackProvider;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void logMcpStatus() {
        ToolCallback[] callbacks;
        try {
            callbacks = toolCallbackProvider.getToolCallbacks();
        } catch (Exception ex) {
            log.warn("MCP startup diagnostic: developer MCP tool discovery failed. "
                    + "Backend startup continues and the developer source may be unavailable until "
                    + "the configured server is reachable.", ex);
            return;
        }

        if (callbacks.length == 0) {
            log.warn("Developer MCP client has no tool callbacks. Verify the developer-server MCP connection "
                    + "is configured and reachable.");
            return;
        }

        log.info("Developer MCP client wired up: {} tool callback(s) available.", callbacks.length);

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
