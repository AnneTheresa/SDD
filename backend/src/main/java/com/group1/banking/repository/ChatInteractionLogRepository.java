package com.group1.banking.repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

import jakarta.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Traceability log for the Savings Insight Chatbot: records each customer query and
 * the generated response (plus whether it was blocked by a guardrail) for QA/demo
 * review. Lives on the chatbot's own Postgres datasource, alongside the pgvector
 * knowledge base - not on the app's primary MySQL/H2 datasource, and not a JPA entity.
 *
 * Deliberately does not persist raw transaction/account data - only the query text,
 * response text, and minimal metadata needed for support/demo traceability, per the
 * "demo-safe" logging requirement.
 */
@Repository
public class ChatInteractionLogRepository {

    private final JdbcTemplate jdbcTemplate;

    public ChatInteractionLogRepository(@Qualifier("chatbotVectorJdbcTemplate") JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void ensureSchema() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS chat_interaction_log (
                    id BIGSERIAL PRIMARY KEY,
                    customer_id BIGINT NOT NULL,
                    query TEXT NOT NULL,
                    response TEXT NOT NULL,
                    outcome VARCHAR(32) NOT NULL,
                    created_at TIMESTAMPTZ NOT NULL
                )
                """);
        // Added after the table was first created, so these have to be ALTERs rather
        // than columns in the CREATE above - CREATE TABLE IF NOT EXISTS is a no-op on
        // an existing table and would silently leave older deployments without them.
        jdbcTemplate.execute("""
                ALTER TABLE chat_interaction_log
                    ADD COLUMN IF NOT EXISTS retrieval_occurred BOOLEAN NOT NULL DEFAULT FALSE,
                    ADD COLUMN IF NOT EXISTS fallback_triggered BOOLEAN NOT NULL DEFAULT FALSE,
                    ADD COLUMN IF NOT EXISTS sources TEXT,
                    ADD COLUMN IF NOT EXISTS tools_used TEXT
                """);
        jdbcTemplate.execute("""
                CREATE INDEX IF NOT EXISTS idx_chat_interaction_log_customer_id
                    ON chat_interaction_log (customer_id)
                """);
    }

    /**
     * Records one chat turn for traceability/QA review (T-BRD 6.1), and returns the
     * generated row ID so the caller can cross-reference it from the shared audit log
     * (CFG-03, see AuditService). The audit log stores a lightweight pointer to a
     * turn (actor, action, outcome); the full query/response text and cited sources
     * stay here, reachable from the audit row via that ID.
     *
     * {@code retrievalOccurred} and {@code fallbackTriggered} are deliberately separate
     * flags rather than being inferred from {@code outcome}: a turn can retrieve from the
     * knowledge base and still fall back to general guidance because no personal data was
     * used, and collapsing both into one column loses that distinction.
     *
     * @param outcome            one of ANSWERED, GUARDRAIL_BLOCKED, FALLBACK, ERROR - lets QA
     *                           validate that unsafe topics were actually blocked
     * @param retrievalOccurred  whether any approved source (accounts, transactions, goals,
     *                           knowledge base) was actually consulted for this turn
     * @param fallbackTriggered  whether the customer received general/controlled guidance
     *                           rather than a fully personalized answer
     * @param sources            plain-language basis for the answer, as shown to the customer;
     *                           stored null when nothing was retrieved
     * @return the generated {@code chat_interaction_log.id} for this row
     */
    public Long log(Long customerId, String query, String response, String outcome,
                boolean retrievalOccurred, boolean fallbackTriggered, List<String> sources,
                List<String> toolsUsed) {
        String joinedSources = (sources == null || sources.isEmpty()) ? null : String.join(" | ", sources);
        String joinedToolsUsed = (toolsUsed == null || toolsUsed.isEmpty()) ? null : String.join(" | ", toolsUsed);
        return jdbcTemplate.queryForObject(
                "INSERT INTO chat_interaction_log (customer_id, query, response, outcome, "
                + "retrieval_occurred, fallback_triggered, sources, tools_used, created_at) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id",
                Long.class,
                customerId, query, response, outcome,
            retrievalOccurred, fallbackTriggered, joinedSources, joinedToolsUsed, Timestamp.from(Instant.now()));
    }
}
