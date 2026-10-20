package com.group1.banking.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.group1.banking.dto.chat.DeveloperChatRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;

class DeveloperChatServiceTest {

    private static final UUID USER_ID = UUID.randomUUID();

    private ChatClient chatClient;
    private ChatClient.ChatClientRequestSpec requestSpec;
    private ChatClient.CallResponseSpec responseSpec;
    private DeveloperChatEvidenceTracker evidenceTracker;
    private DeveloperChatService service;

    @BeforeEach
    void setUp() {
        chatClient = mock(ChatClient.class);
        requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        responseSpec = mock(ChatClient.CallResponseSpec.class);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(responseSpec);
        evidenceTracker = new DeveloperChatEvidenceTracker();
        service = new DeveloperChatService(chatClient, evidenceTracker);
    }

    @Test
    void returnsModelAnswerWithToolReportedEvidence() {
        when(responseSpec.content()).thenAnswer(invocation -> {
            evidenceTracker.record("SOURCE: type=github; path=src/App.java; "
                    + "url=https://github.com/owner/repo/blob/main/src/App.java; revision=branch:main");
            return "The app route is defined in the cited file.";
        });
        DeveloperChatRequest request = request("Where is the app route?", null);

        var response = service.ask(USER_ID, request);

        assertThat(response.answer()).isEqualTo("The app route is defined in the cited file.");
        assertThat(response.sourceStatus()).isEqualTo("AVAILABLE");
        assertThat(response.sources())
                .singleElement()
                .satisfies(source -> {
                    assertThat(source.path()).isEqualTo("src/App.java");
                    assertThat(source.revision()).isEqualTo("branch:main");
                });
        assertThat(response.sessionId()).isNotBlank();
    }

    @Test
    void followUpUsesTheSameSessionAndPriorTurnContext() {
        List<String> prompts = new ArrayList<>();
        when(requestSpec.user(anyString())).thenAnswer(invocation -> {
            prompts.add(invocation.getArgument(0, String.class));
            return requestSpec;
        });
        when(responseSpec.content()).thenAnswer(invocation -> {
            evidenceTracker.record("SOURCE: type=documentation; path=contributor-guide.md; "
                    + "url=; revision=working-tree");
            return "A source-backed answer.";
        });

        var first = service.ask(USER_ID, request("How do I run tests?", null));
        var second = service.ask(USER_ID, request("What about just the backend?", first.sessionId()));

        assertThat(second.sessionId()).isEqualTo(first.sessionId());
        assertThat(prompts).hasSize(2);
        assertThat(prompts.get(1)).contains("How do I run tests?", "A source-backed answer.");
        assertThat(prompts.get(1)).endsWith("Current contributor question:\nWhat about just the backend?");
    }

    @Test
    void doesNotReturnUnsupportedModelTextWithoutAnyCitation() {
        when(responseSpec.content()).thenReturn("An unsupported repository-specific claim.");

        var response = service.ask(USER_ID, request("What does this app do?", null));

        assertThat(response.answer()).contains("couldn't substantiate");
        assertThat(response.sources()).isEmpty();
        assertThat(response.sourceStatus()).isEqualTo("NO_EVIDENCE");
    }

    @Test
    void reportsUnavailableSourcesWhenTheChatClientFails() {
        when(responseSpec.content()).thenThrow(new IllegalStateException("remote details must not be exposed"));

        var response = service.ask(USER_ID, request("Explain the service layer", null));

        assertThat(response.answer()).contains("couldn't reach");
        assertThat(response.answer()).doesNotContain("remote details");
        assertThat(response.sources()).isEmpty();
        assertThat(response.sourceStatus()).isEqualTo("UNAVAILABLE");
    }

    private DeveloperChatRequest request(String message, String sessionId) {
        DeveloperChatRequest request = new DeveloperChatRequest();
        request.setMessage(message);
        request.setSessionId(sessionId);
        return request;
    }
}
