package com.group1.banking.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

class ChatInteractionLogRepositoryTest {

    @Test
    void ensureSchema_shouldAddToolsUsedColumn() {
        JdbcTemplate jdbcTemplate = org.mockito.Mockito.mock(JdbcTemplate.class);
        ChatInteractionLogRepository repository = new ChatInteractionLogRepository(jdbcTemplate);

        repository.ensureSchema();

        verify(jdbcTemplate, atLeastOnce()).execute(contains("tools_used"));
    }

    @Test
    void log_shouldPersistToolsUsedAsPipeJoinedString() {
        JdbcTemplate jdbcTemplate = org.mockito.Mockito.mock(JdbcTemplate.class);
        ChatInteractionLogRepository repository = new ChatInteractionLogRepository(jdbcTemplate);
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class),
            any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(10L);

        Long id = repository.log(42L, "question", "answer", "ANSWERED", true, false,
                List.of("Savings knowledge base: 07-gics-explained.md"),
                List.of("getGicRates", "searchSavingsKnowledgeBase"));

        assertThat(id).isEqualTo(10L);
        verify(jdbcTemplate).queryForObject(anyString(), eq(Long.class),
                eq(42L), eq("question"), eq("answer"), eq("ANSWERED"),
                eq(true), eq(false), eq("Savings knowledge base: 07-gics-explained.md"),
                eq("getGicRates | searchSavingsKnowledgeBase"), any(Timestamp.class));
    }

    @Test
    void log_shouldPersistNullToolsUsed_whenNoToolsInvoked() {
        JdbcTemplate jdbcTemplate = org.mockito.Mockito.mock(JdbcTemplate.class);
        ChatInteractionLogRepository repository = new ChatInteractionLogRepository(jdbcTemplate);
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class),
            any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(11L);

        Long id = repository.log(42L, "hello", "Hi there!", "FALLBACK", false, true,
                List.of(), List.of());

        assertThat(id).isEqualTo(11L);
        verify(jdbcTemplate).queryForObject(anyString(), eq(Long.class),
                eq(42L), eq("hello"), eq("Hi there!"), eq("FALLBACK"),
                eq(false), eq(true), eq((Object) null), eq((Object) null), any(Timestamp.class));
    }
}
