package com.group1.banking.service.impl;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * US4: ingestion is idempotent per source file (the "source" metadata key, set to the
 * article's filename), not gated on whether the vector table has any content at all -
 * so a brand-new article ingests on the next startup even if older articles are
 * already seeded, and a startup with nothing new to ingest never re-adds anything.
 *
 * Runs against the real classpath:knowledge-base/*.md articles (via the real
 * PathMatchingResourcePatternResolver/TextReader/TokenTextSplitter pipeline) - only
 * the persistence layer (VectorStore, JdbcTemplate) is mocked - so these tests don't
 * hardcode the current article filenames or count.
 */
class KnowledgeBaseIngestionRunnerTest {

    private VectorStore vectorStore;
    private JdbcTemplate chatbotVectorJdbcTemplate;
    private KnowledgeBaseIngestionRunner runner;

    @BeforeEach
    void setUp() {
        vectorStore = mock(VectorStore.class);
        chatbotVectorJdbcTemplate = mock(JdbcTemplate.class);
        runner = new KnowledgeBaseIngestionRunner(vectorStore, chatbotVectorJdbcTemplate);
    }

    @Test
    void run_shouldIngestAllArticles_onFirstRun() {
        when(chatbotVectorJdbcTemplate.queryForObject(anyString(), eq(Integer.class), anyString()))
                .thenReturn(0);

        runner.run(null);

        List<Document> ingested = captureIngestedChunks();
        assertThat(ingested).isNotEmpty();
    }

    @Test
    void run_shouldIngestNothing_whenEveryArticleAlreadyIngested() {
        when(chatbotVectorJdbcTemplate.queryForObject(anyString(), eq(Integer.class), anyString()))
                .thenReturn(1);

        runner.run(null);

        verify(vectorStore, never()).add(any());
    }

    @Test
    void run_shouldIngestOnlyTheNewArticle_whenOthersAreAlreadySeeded() {
        String alreadyIngestedFile = "01-emergency-fund-basics.md";
        // A single stub with thenAnswer, rather than two separate eq()/argThat() stubs for the
        // varargs "source" parameter, avoids relying on Mockito's per-vararg-element matcher
        // resolution - invocation.getArgument(2) reliably returns the actual flattened value
        // regardless of that ambiguity.
        when(chatbotVectorJdbcTemplate.queryForObject(anyString(), eq(Integer.class), any()))
                .thenAnswer(invocation -> {
                    String sourceFilename = invocation.getArgument(2);
                    return alreadyIngestedFile.equals(sourceFilename) ? 1 : 0;
                });

        runner.run(null);

        List<Document> ingested = captureIngestedChunks();
        assertThat(ingested).isNotEmpty();
        assertThat(ingested).noneMatch(doc -> alreadyIngestedFile.equals(doc.getMetadata().get("source")));
    }

    @SuppressWarnings("unchecked")
    private List<Document> captureIngestedChunks() {
        ArgumentCaptor<List<Document>> captor = ArgumentCaptor.forClass(List.class);
        verify(vectorStore).add(captor.capture());
        return captor.getValue();
    }
}
