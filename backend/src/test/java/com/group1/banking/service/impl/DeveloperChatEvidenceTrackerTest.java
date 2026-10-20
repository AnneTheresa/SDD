package com.group1.banking.service.impl;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.ai.util.JsonHelper;

import static org.assertj.core.api.Assertions.assertThat;

class DeveloperChatEvidenceTrackerTest {

    @Test
    void recordsSourcesAndStatusesFromSerializedMcpToolResults() {
        JsonHelper json = new JsonHelper();
        DeveloperChatEvidenceTracker tracker = new DeveloperChatEvidenceTracker();
        tracker.beginTurn();
        String mcpText = "SOURCE: type=documentation; path=SETUP.md; url=; revision=working-tree\n"
                + "CONTENT:\nRun the developer MCP server from mcpdevserver/.";
        String serializedResult = json.toJson(List.of(
                Map.of("type", "text", "text", json.toJson(mcpText))));

        tracker.record(serializedResult);
        tracker.record(json.toJson(List.of(Map.of(
                "type", "text",
                "text", json.toJson("SOURCE_STATUS: unavailable; source=github; reason=network-error")))));

        var evidence = tracker.snapshot();
        assertThat(evidence.sources()).singleElement()
                .satisfies(source -> assertThat(source.path()).isEqualTo("SETUP.md"));
        assertThat(evidence.statuses()).containsEntry("github", "unavailable");
    }
}
