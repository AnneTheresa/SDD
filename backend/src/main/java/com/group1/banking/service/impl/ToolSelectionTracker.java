package com.group1.banking.service.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Component;

/**
 * Tracks, per chat turn, which tool names were actually invoked.
 *
 * Mirrors the reset-and-drain lifecycle used by the other per-turn trackers so
 * SavingsInsightChatService can persist the final tool list on the chat log row.
 *
 * The ThreadLocal is an instance field, not static: in production there is exactly
 * one singleton instance (this is a {@code @Component}), so that makes no functional
 * difference there, but it matters for tests - a static ThreadLocal is shared by every
 * {@code new ToolSelectionTracker()}, so two unrelated test instances on the same
 * Surefire thread would otherwise see each other's recorded tool names.
 */
@Component
public class ToolSelectionTracker {

    private final ThreadLocal<List<String>> toolNames = ThreadLocal.withInitial(ArrayList::new);

    public void reset() {
        toolNames.remove();
    }

    public void recordTool(String toolName) {
        if (toolName == null || toolName.isBlank()) {
            return;
        }
        toolNames.get().add(toolName);
    }

    public List<String> drainToolsUsed() {
        List<String> tools = new ArrayList<>(toolNames.get());
        toolNames.remove();
        return tools.isEmpty() ? List.of() : Collections.unmodifiableList(tools);
    }
}