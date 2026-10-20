package com.group1.banking.service.impl;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.group1.banking.dto.chat.DeveloperChatSource;
import org.springframework.stereotype.Component;

@Component
public class DeveloperChatEvidenceTracker {

    private final ThreadLocal<TurnEvidence> currentTurn = new ThreadLocal<>();

    public void beginTurn() {
        currentTurn.set(new TurnEvidence());
    }

    public void record(String toolOutput) {
        TurnEvidence evidence = currentTurn.get();
        if (evidence == null || toolOutput == null) {
            return;
        }
        for (String line : toolOutput.split("\\R")) {
            if (line.startsWith("SOURCE_STATUS:")) {
                String status = line.substring("SOURCE_STATUS:".length()).trim().split(";", 2)[0];
                String source = value(line, "source");
                if (!source.isBlank()) {
                    evidence.statuses.put(source, status);
                }
            } else if (line.startsWith("SOURCE:")) {
                String type = value(line, "type");
                String path = value(line, "path");
                if (!type.isBlank() && !path.isBlank()) {
                    DeveloperChatSource source = new DeveloperChatSource(
                            type, path, value(line, "url"), value(line, "revision"));
                    evidence.sources.putIfAbsent(type + "\n" + path + "\n" + source.revision(), source);
                }
            }
        }
    }

    public TurnEvidence snapshot() {
        TurnEvidence evidence = currentTurn.get();
        return evidence == null ? new TurnEvidence() : evidence.copy();
    }

    public void clear() {
        currentTurn.remove();
    }

    private String value(String line, String key) {
        String prefix = key + "=";
        int markerSeparator = line.indexOf(':');
        String fields = markerSeparator < 0 ? line : line.substring(markerSeparator + 1);
        for (String part : fields.split(";")) {
            String trimmed = part.trim();
            if (trimmed.startsWith(prefix)) {
                return trimmed.substring(prefix.length()).trim();
            }
        }
        return "";
    }

    public static class TurnEvidence {

        private final Map<String, DeveloperChatSource> sources = new LinkedHashMap<>();
        private final Map<String, String> statuses = new LinkedHashMap<>();

        public List<DeveloperChatSource> sources() {
            return List.copyOf(sources.values());
        }

        public Map<String, String> statuses() {
            return Map.copyOf(statuses);
        }

        private TurnEvidence copy() {
            TurnEvidence copy = new TurnEvidence();
            copy.sources.putAll(sources);
            copy.statuses.putAll(statuses);
            return copy;
        }
    }
}
