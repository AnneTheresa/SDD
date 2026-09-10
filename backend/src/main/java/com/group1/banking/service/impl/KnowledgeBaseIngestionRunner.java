package com.group1.banking.service.impl;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.group1.banking.config.ChatbotAiConfig;

/**
 * Seeds the pgvector knowledge base from the curated savings articles under
 * {@code resources/knowledge-base/} on startup. Idempotent per source file: each
 * article is only ingested if no row for its filename (the "source" metadata key)
 * already exists in the vector table, so adding a brand-new article ingests just that
 * article on the next startup rather than being skipped because the table already has
 * unrelated content in it - and re-ingesting an already-seeded article never happens,
 * so restarts don't duplicate entries either.
 */
@Component
public class KnowledgeBaseIngestionRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeBaseIngestionRunner.class);
    private static final String KNOWLEDGE_BASE_LOCATION_PATTERN = "classpath:knowledge-base/*.md";

    private final VectorStore vectorStore;
    private final JdbcTemplate chatbotVectorJdbcTemplate;

    public KnowledgeBaseIngestionRunner(VectorStore vectorStore,
                                         @Qualifier("chatbotVectorJdbcTemplate") JdbcTemplate chatbotVectorJdbcTemplate) {
        this.vectorStore = vectorStore;
        this.chatbotVectorJdbcTemplate = chatbotVectorJdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources(KNOWLEDGE_BASE_LOCATION_PATTERN);

            if (resources.length == 0) {
                log.warn("No knowledge base articles found at {}", KNOWLEDGE_BASE_LOCATION_PATTERN);
                return;
            }

            List<Resource> newResources = new ArrayList<>();
            int alreadyIngestedCount = 0;
            for (Resource resource : resources) {
                if (isAlreadyIngested(resource.getFilename())) {
                    alreadyIngestedCount++;
                } else {
                    newResources.add(resource);
                }
            }

            if (newResources.isEmpty()) {
                log.info("Savings knowledge base: all {} article(s) already ingested; "
                        + "nothing new to seed on startup.", alreadyIngestedCount);
                return;
            }

            TokenTextSplitter splitter = new TokenTextSplitter();
            List<Document> chunks = new ArrayList<>();

            for (Resource resource : newResources) {
                TextReader reader = new TextReader(resource);
                reader.getCustomMetadata().put("source", resource.getFilename());
                reader.getCustomMetadata().put("type", "savings-knowledge-base");
                List<Document> docs = reader.get();
                chunks.addAll(splitter.apply(docs));
            }

            vectorStore.add(chunks);
            log.info("Ingested {} chunk(s) from {} new savings knowledge base article(s) "
                    + "({} already ingested and left untouched).",
                    chunks.size(), newResources.size(), alreadyIngestedCount);
        } catch (Exception ex) {
            // Non-fatal: the chatbot degrades to structured-data-only / fallback
            // responses if the knowledge base couldn't be seeded, rather than
            // failing application startup.
            log.error("Failed to seed savings knowledge base; chatbot will run without the "
                    + "missing article(s) until this is resolved.", ex);
        }
    }

    /**
     * Per-file dedupe check: the "source" metadata key (set to the article's filename
     * below) is the dedupe key, matching data-model.md. Uses the {@code ->>} JSON text
     * extraction operator, which Postgres supports on the vector store's {@code json}
     * metadata column the same as it does on {@code jsonb}.
     */
    private boolean isAlreadyIngested(String sourceFilename) {
        Integer count = chatbotVectorJdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + ChatbotAiConfig.KNOWLEDGE_BASE_TABLE + " WHERE metadata->>'source' = ?",
                Integer.class, sourceFilename);
        return count != null && count > 0;
    }
}
