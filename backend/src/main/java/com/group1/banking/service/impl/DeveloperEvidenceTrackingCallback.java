package com.group1.banking.service.impl;

import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.metadata.ToolMetadata;

public class DeveloperEvidenceTrackingCallback implements ToolCallback {

    private final ToolCallback delegate;
    private final DeveloperChatEvidenceTracker evidenceTracker;

    public DeveloperEvidenceTrackingCallback(
            ToolCallback delegate,
            DeveloperChatEvidenceTracker evidenceTracker) {
        this.delegate = delegate;
        this.evidenceTracker = evidenceTracker;
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
        return track(delegate.call(toolInput));
    }

    @Override
    public String call(String toolInput, ToolContext toolContext) {
        return track(delegate.call(toolInput, toolContext));
    }

    private String track(String output) {
        evidenceTracker.record(output);
        return output;
    }
}
