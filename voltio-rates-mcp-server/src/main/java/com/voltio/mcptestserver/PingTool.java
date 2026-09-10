package com.voltio.mcptestserver;

import java.time.Instant;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

/**
 * A diagnostic tool this server exposes over MCP alongside its real {@link GicRateTool}
 * tool. Deliberately trivial and side-effect-free - its job is to give
 * backend/.../config/McpClientDiagnostics.java a fast, unambiguous way to confirm a
 * full MCP client-server round trip (initialize -> list tools -> call tool) is
 * working, independent of whether GIC rate data itself changes or fails.
 */
@Component
public class PingTool {

    @Tool(description = "Returns a fixed reply plus this server's current time. "
            + "Used only to prove an MCP client can reach and call this server - "
            + "it has no other function.")
    public String ping() {
        return "pong from voltio-rates-mcp-server at " + Instant.now();
    }
}
