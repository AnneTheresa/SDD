package com.group1.banking.service.impl;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import com.group1.banking.dto.chat.DeveloperChatRequest;
import com.group1.banking.dto.chat.DeveloperChatResponse;
import com.group1.banking.dto.chat.DeveloperChatSource;
import com.group1.banking.service.impl.DeveloperChatEvidenceTracker.TurnEvidence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.util.JsonHelper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class DeveloperChatService {

    private static final Logger log = LoggerFactory.getLogger(DeveloperChatService.class);
    private static final int MAX_CONTEXT_TURNS = 8;
    private static final int MAX_SESSIONS = 1_000;
    private static final Duration SESSION_IDLE_TIMEOUT = Duration.ofMinutes(30);
    private static final String NO_EVIDENCE_ANSWER =
            "I couldn't substantiate an answer from the configured documentation or repository sources. "
                    + "Please retry later or check the configured sources directly.";
    private static final String SOURCE_UNAVAILABLE_ANSWER =
            "I couldn't reach the configured documentation or repository sources, so I can't verify "
                    + "a repository-specific answer right now. Please retry later.";
    private static final String DOCUMENTATION_FALLBACK_PREFIX =
            "I couldn't verify this against the live repository. The contributor documentation provides "
                    + "the following relevant information:";
    private static final String DOCUMENTATION_TOOL_NAME = "developer_search_documentation";
    private static final JsonHelper JSON = new JsonHelper();

    private final ChatClient chatClient;
    private final DeveloperChatEvidenceTracker evidenceTracker;
    private final ToolCallbackProvider developerMcpToolCallbackProvider;
    private final Map<SessionKey, Conversation> conversations = new ConcurrentHashMap<>();

    public DeveloperChatService(
            ChatClient developerChatClient,
            DeveloperChatEvidenceTracker evidenceTracker,
            @Qualifier("developerMcpToolCallbackProvider")
            ToolCallbackProvider developerMcpToolCallbackProvider) {
        this.chatClient = developerChatClient;
        this.evidenceTracker = evidenceTracker;
        this.developerMcpToolCallbackProvider = developerMcpToolCallbackProvider;
    }

    public DeveloperChatResponse ask(UUID userId, DeveloperChatRequest request) {
        UUID sessionId = request.getSessionId() == null || request.getSessionId().isBlank()
                ? UUID.randomUUID()
                : UUID.fromString(request.getSessionId());
        SessionKey key = new SessionKey(userId, sessionId);
        cleanupExpiredSessions();
        Conversation conversation = conversations.computeIfAbsent(key, ignored -> new Conversation());

        synchronized (conversation) {
            conversation.lastActivity = Instant.now();
            evidenceTracker.beginTurn();
            String answer;
            TurnEvidence evidence;
            boolean chatFailed = false;
            try {
                String prompt = promptWithContext(conversation.turns, request.getMessage());
                answer = chatClient.prompt().user(prompt).call().content();
                evidence = evidenceTracker.snapshot();
            } catch (RuntimeException ex) {
                log.warn("Developer chat failed for userId={} sessionId={} ({})",
                        userId, sessionId, ex.getClass().getSimpleName());
                answer = null;
                evidence = evidenceTracker.snapshot();
                chatFailed = true;
            } finally {
                evidenceTracker.clear();
            }

            if (chatFailed || needsDocumentationFallback(evidence)) {
                evidenceTracker.beginTurn();
                try {
                    String fallbackAnswer = searchDocumentationFallback(request.getMessage());
                    TurnEvidence fallbackEvidence = evidenceTracker.snapshot();
                    if (fallbackAnswer != null) {
                        answer = fallbackAnswer;
                        evidence = evidence.with(fallbackEvidence);
                    }
                } catch (RuntimeException ex) {
                    log.warn("Developer documentation fallback failed for userId={} sessionId={} ({})",
                            userId, sessionId, ex.getClass().getSimpleName());
                } finally {
                    evidenceTracker.clear();
                }
            }

            String status = sourceStatus(evidence);
            List<DeveloperChatSource> sources = evidence.sources();
            if (chatFailed && sources.isEmpty() && status.equals("NO_EVIDENCE")) {
                status = "UNAVAILABLE";
            }
            if (sources.isEmpty()) {
                answer = status.equals("UNAVAILABLE") ? SOURCE_UNAVAILABLE_ANSWER : NO_EVIDENCE_ANSWER;
            } else if (answer == null || answer.isBlank()) {
                answer = NO_EVIDENCE_ANSWER;
                status = "NO_EVIDENCE";
                sources = List.of();
            }

            String citations = sources.stream()
                    .map(source -> source.sourceType() + ": " + source.path())
                    .reduce((left, right) -> left + ", " + right)
                    .orElse("none");
            conversation.turns.addLast(new Turn(request.getMessage(), answer, citations));
            while (conversation.turns.size() > MAX_CONTEXT_TURNS) {
                conversation.turns.removeFirst();
            }
            return new DeveloperChatResponse(sessionId.toString(), answer, sources, status);
        }
    }

    private boolean needsDocumentationFallback(TurnEvidence evidence) {
        boolean hasDocumentation = evidence.sources().stream()
                .anyMatch(source -> source.sourceType().equalsIgnoreCase("documentation"));
        boolean githubUnavailable = "unavailable".equals(evidence.statuses().get("github"));
        return !hasDocumentation && (githubUnavailable || evidence.sources().isEmpty());
    }

    private String searchDocumentationFallback(String query) {
        ToolCallback searchTool = java.util.Arrays.stream(developerMcpToolCallbackProvider.getToolCallbacks())
                .filter(callback -> DOCUMENTATION_TOOL_NAME.equals(callback.getToolDefinition().name()))
                .findFirst()
                .orElse(null);
        if (searchTool == null) {
            return null;
        }

        String output = searchTool.call(JSON.toJson(Map.of("query", query)));
        evidenceTracker.record(output);
        TurnEvidence evidence = evidenceTracker.snapshot();
        if (evidence.sources().stream()
                .noneMatch(source -> source.sourceType().equalsIgnoreCase("documentation"))) {
            return null;
        }

        String excerpts = String.join("\n", McpToolResultText.extractTexts(output)).lines()
                .filter(line -> !line.startsWith("SOURCE:") && !line.startsWith("SOURCE_STATUS:")
                        && !line.equals("CONTENT:"))
                .collect(Collectors.joining("\n"))
                .trim();
        return excerpts.isBlank() ? null : DOCUMENTATION_FALLBACK_PREFIX + "\n\n" + excerpts;
    }

    private String promptWithContext(Deque<Turn> turns, String message) {
        if (turns.isEmpty()) {
            return message;
        }
        StringBuilder prompt = new StringBuilder("Prior turns in this session (user and assistant text only):\n");
        for (Turn turn : turns) {
            prompt.append("Contributor: ").append(turn.question()).append('\n')
                    .append("Assistant: ").append(turn.answer())
                    .append("\nSources: ").append(turn.citations()).append('\n');
        }
        return prompt.append("\nCurrent contributor question:\n").append(message).toString();
    }

    private String sourceStatus(TurnEvidence evidence) {
        if (!evidence.sources().isEmpty()) {
            boolean partial = evidence.statuses().values().stream()
                    .anyMatch(status -> status.equals("unavailable") || status.equals("no_evidence"));
            return partial ? "PARTIAL" : "AVAILABLE";
        }
        if (evidence.statuses().containsValue("unavailable")) {
            return "UNAVAILABLE";
        }
        return "NO_EVIDENCE";
    }

    private void cleanupExpiredSessions() {
        Instant cutoff = Instant.now().minus(SESSION_IDLE_TIMEOUT);
        conversations.entrySet().removeIf(entry -> entry.getValue().lastActivity.isBefore(cutoff));
        if (conversations.size() > MAX_SESSIONS) {
            List<Map.Entry<SessionKey, Conversation>> oldest = new ArrayList<>(conversations.entrySet());
            oldest.sort((left, right) -> left.getValue().lastActivity.compareTo(right.getValue().lastActivity));
            int excess = conversations.size() - MAX_SESSIONS;
            for (int index = 0; index < excess; index++) {
                conversations.remove(oldest.get(index).getKey(), oldest.get(index).getValue());
            }
        }
    }

    private record SessionKey(UUID userId, UUID sessionId) {
    }

    private record Turn(String question, String answer, String citations) {
    }

    private static class Conversation {
        private final Deque<Turn> turns = new ArrayDeque<>();
        private volatile Instant lastActivity = Instant.now();
    }
}
