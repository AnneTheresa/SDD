package com.voltio.mcptestserver;

import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registers {@link PingTool}'s {@code @Tool}-annotated method with Spring AI's
 * MCP server autoconfiguration, which serves whatever {@link ToolCallbackProvider}
 * bean(s) it finds over MCP (SSE, via spring-ai-starter-mcp-server-webmvc).
 */
@Configuration
public class McpServerToolConfig {

    @Bean
    public ToolCallbackProvider testServerTools(PingTool pingTool, GicRateTool gicRateTool) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(pingTool, gicRateTool)
                .build();
    }
}
