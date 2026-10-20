package com.group1.banking.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.group1.banking.dto.chat.DeveloperChatRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.util.JsonHelper;

class DeveloperChatServiceTest {

    private static final UUID USER_ID = UUID.randomUUID();

    private ChatClient chatClient;
    private ChatClient.ChatClientRequestSpec requestSpec;
    private ChatClient.CallResponseSpec responseSpec;
    private DeveloperChatEvidenceTracker evidenceTracker;
    private ToolCallbackProvider developerMcpToolCallbackProvider;
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
        developerMcpToolCallbackProvider = () -> new ToolCallback[0];
        service = new DeveloperChatService(chatClient, evidenceTracker, developerMcpToolCallbackProvider);
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
    void doesNotReusePriorTurnCitationsAsCurrentEvidence() {
        when(responseSpec.content()).thenAnswer(new org.mockito.stubbing.Answer<String>() {
            private int calls;

            @Override
            public String answer(org.mockito.invocation.InvocationOnMock invocation) {
                if (calls++ == 0) {
                    evidenceTracker.record("SOURCE: type=documentation; path=SETUP.md; url=; revision=working-tree");
                    return "The setup guide describes the local workflow.";
                }
                return "An unsupported follow-up claim.";
            }
        });
        var first = service.ask(USER_ID, request("How do I start locally?", null));

        var second = service.ask(USER_ID, request("What exact port is it on?", first.sessionId()));

        assertThat(second.answer()).contains("couldn't substantiate");
        assertThat(second.sources()).isEmpty();
        assertThat(second.sourceStatus()).isEqualTo("NO_EVIDENCE");
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

    @Test
    void usesDocumentationExcerptWhenChatCallFails() {
        ToolCallback documentationSearch = documentationSearch(
                "SOURCE: type=documentation; path=SETUP.md; url=; revision=working-tree\n"
                        + "CONTENT:\nStart PostgreSQL, then run the backend from backend/.");
        developerMcpToolCallbackProvider = () -> new ToolCallback[] { documentationSearch };
        service = new DeveloperChatService(chatClient, evidenceTracker, developerMcpToolCallbackProvider);
        when(responseSpec.content()).thenThrow(new IllegalStateException("chat unavailable"));

        var response = service.ask(USER_ID, request("How do I run the backend?", null));

        assertThat(response.answer()).contains("Start PostgreSQL", "backend/");
        assertThat(response.answer()).doesNotContain("couldn't reach");
        assertThat(response.sourceStatus()).isEqualTo("AVAILABLE");
        assertThat(response.sources()).singleElement()
                .satisfies(source -> assertThat(source.path()).isEqualTo("SETUP.md"));
    }

    @Test
    void fallsBackToDocumentationWhenGitHubIsUnavailable() {
        ToolCallback documentationSearch = documentationSearch(
                "SOURCE: type=documentation; path=onboarding/README.md; url=; revision=working-tree\n"
                        + "CONTENT:\nThe contributor guide explains the local development workflow.");
        developerMcpToolCallbackProvider = () -> new ToolCallback[] { documentationSearch };
        service = new DeveloperChatService(chatClient, evidenceTracker, developerMcpToolCallbackProvider);
        when(responseSpec.content()).thenAnswer(invocation -> {
            evidenceTracker.record("SOURCE_STATUS: unavailable; source=github; reason=network-error");
            return "The repository is currently unavailable.";
        });

        var response = service.ask(USER_ID, request("How does local development work?", null));

        assertThat(response.answer()).contains("contributor guide", "local development workflow");
        assertThat(response.sourceStatus()).isEqualTo("PARTIAL");
        assertThat(response.sources()).singleElement()
                .satisfies(source -> assertThat(source.path()).isEqualTo("onboarding/README.md"));
    }

    private ToolCallback documentationSearch(String toolText) {
        ToolCallback callback = mock(ToolCallback.class);
        ToolDefinition definition = mock(ToolDefinition.class);
        when(definition.name()).thenReturn("developer_search_documentation");
        when(callback.getToolDefinition()).thenReturn(definition);
        JsonHelper json = new JsonHelper();
        String encodedText = json.toJson(toolText);
        when(callback.call(anyString())).thenReturn(json.toJson(List.of(
                Map.of("type", "text", "text", encodedText))));
        return callback;
    }

    private DeveloperChatRequest request(String message, String sessionId) {
        DeveloperChatRequest request = new DeveloperChatRequest();
        request.setMessage(message);
        request.setSessionId(sessionId);
        return request;
    }
}
