package com.group1.banking.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.ai.util.JsonHelper;
import tools.jackson.core.JacksonException;

final class McpToolResultText {

    private static final JsonHelper JSON = new JsonHelper();

    private McpToolResultText() {
    }

    static List<String> extractTexts(String result) {
        List<String> texts = new ArrayList<>();
        collect(result, texts, 0);
        return List.copyOf(texts);
    }

    private static void collect(Object value, List<String> texts, int depth) {
        if (value == null || depth > 4) {
            return;
        }
        if (value instanceof String text) {
            String trimmed = text.trim();
            if (looksLikeJson(trimmed)) {
                try {
                    Object decoded = JSON.fromJson(trimmed, Object.class);
                    if (!(decoded instanceof String decodedText && decodedText.equals(text))) {
                        collect(decoded, texts, depth + 1);
                        return;
                    }
                } catch (JacksonException ex) {
                    texts.add(text);
                    return;
                }
            }
            texts.add(text);
        } else if (value instanceof Iterable<?> values) {
            values.forEach(item -> collect(item, texts, depth + 1));
        } else if (value instanceof Map<?, ?> fields) {
            Object text = fields.get("text");
            if (text != null) {
                collect(text, texts, depth + 1);
            } else {
                fields.values().forEach(item -> collect(item, texts, depth + 1));
            }
        }
    }

    private static boolean looksLikeJson(String value) {
        return value.length() > 1
                && (value.startsWith("[") || value.startsWith("{") || value.startsWith("\""));
    }
}
