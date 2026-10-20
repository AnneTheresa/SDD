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

import com.group1.banking.dto.chat.DeveloperChatRequest;
import com.group1.banking.dto.chat.DeveloperChatResponse;
import com.group1.banking.dto.chat.DeveloperChatSource;
import com.group1.banking.service.impl.DeveloperChatEvidenceTracker.TurnEvidence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private final ChatClient chatClient;
    private final DeveloperChatEvidenceTracker evidenceTracker;
    private final Map<SessionKey, Conversation> conversations = new ConcurrentHashMap<>();

    public DeveloperChatService(
            ChatClient developerChatClient,
            DeveloperChatEvidenceTracker evidenceTracker) {
        this.chatClient = developerChatClient;
        this.evidenceTracker = evidenceTracker;
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
            try {
                String prompt = promptWithContext(conversation.turns, request.getMessage());
                answer = chatClient.prompt().user(prompt).call().content();
                evidence = evidenceTracker.snapshot();
            } catch (RuntimeException ex) {
                log.warn("Developer chat failed for userId={} sessionId={} ({})",
                        userId, sessionId, ex.getClass().getSimpleName());
                return new DeveloperChatResponse(
                        sessionId.toString(), SOURCE_UNAVAILABLE_ANSWER, List.of(), "UNAVAILABLE");
            } finally {
                evidenceTracker.clear();
            }

            String status = sourceStatus(evidence);
            List<DeveloperChatSource> sources = evidence.sources();
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
