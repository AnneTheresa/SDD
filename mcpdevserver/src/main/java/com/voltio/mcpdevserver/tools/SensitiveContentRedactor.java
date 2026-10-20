package com.voltio.mcpdevserver.tools;

import java.util.regex.Pattern;

final class SensitiveContentRedactor {

    private static final Pattern PRIVATE_KEY = Pattern.compile(
            "-----BEGIN [A-Z ]*PRIVATE KEY-----[\\s\\S]*?-----END [A-Z ]*PRIVATE KEY-----");
    private static final Pattern GITHUB_TOKEN = Pattern.compile("\\b(?:gh[pousr]_[A-Za-z0-9]{20,}|github_pat_[A-Za-z0-9_]{20,})\\b");
    private static final Pattern AWS_ACCESS_KEY = Pattern.compile("\\bAKIA[0-9A-Z]{16}\\b");
    private static final Pattern SECRET_ASSIGNMENT = Pattern.compile(
            "(?i)\\b(password|token|secret|api[_-]?key)\\b(\\s*[:=]\\s*)([\"']?)[^\\s\"',;]+");
    private static final Pattern EVIDENCE_MARKER = Pattern.compile(
            "(?m)^(\\s*)(SOURCE(?:_STATUS)?):");

    private SensitiveContentRedactor() {
    }

    static String redact(String content) {
        String redacted = PRIVATE_KEY.matcher(content).replaceAll("[REDACTED PRIVATE KEY]");
        redacted = GITHUB_TOKEN.matcher(redacted).replaceAll("[REDACTED TOKEN]");
        redacted = AWS_ACCESS_KEY.matcher(redacted).replaceAll("[REDACTED ACCESS KEY]");
        return SECRET_ASSIGNMENT.matcher(redacted).replaceAll("$1$2[REDACTED]");
    }

    static String escapeEvidenceMarkers(String content) {
        return EVIDENCE_MARKER.matcher(content).replaceAll("$1UNTRUSTED CONTENT $2:");
    }
}
