package com.group1.banking.service.impl;

import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.metadata.ToolMetadata;

/**
 * Decorates an MCP-sourced {@link ToolCallback} so every invocation records itself on
 * the shared {@link ToolSelectionTracker}, the same way the in-process
 * {@code @Tool}-annotated methods do. Without this bridge, tools served through an MCP
 * {@code ToolCallbackProvider} (e.g. the rates server's {@code getGicRates}) would be
 * invisible to chat_interaction_log.tools_used even though the model actually called them.
 *
 * Delegates every other behaviour (definition, metadata, both call overloads) to the
 * wrapped callback unchanged - this class only observes, it never changes what a tool
 * does or how it's described to the model.
 */
public class ToolTrackingCallback implements ToolCallback {

    private final ToolCallback delegate;
    private final ToolSelectionTracker toolSelectionTracker;

    public ToolTrackingCallback(ToolCallback delegate, ToolSelectionTracker toolSelectionTracker) {
        this.delegate = delegate;
        this.toolSelectionTracker = toolSelectionTracker;
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return delegate.getToolDefinition();
    }

    @Override
    public ToolMetadata getToolMetadata() {
        return delegate.getToolMetadata();
    }

    @Override
    public String call(String toolInput) {
        toolSelectionTracker.recordTool(delegate.getToolDefinition().name());
        return delegate.call(toolInput);
    }

    @Override
    public String call(String toolInput, ToolContext toolContext) {
        toolSelectionTracker.recordTool(delegate.getToolDefinition().name());
        return delegate.call(toolInput, toolContext);
    }
}
