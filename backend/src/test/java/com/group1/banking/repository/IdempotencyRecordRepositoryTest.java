package com.group1.banking.repository;

import com.group1.banking.entity.IdempotencyRecord;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class IdempotencyRecordRepositoryTest {

    @Autowired
    private IdempotencyRecordRepository repository;

    @Test
    void findByStorageKey_shouldReturnRecord_whenExists() {
        repository.save(buildRecord("sk-001", "ik-001"));

        Optional<IdempotencyRecord> found = repository.findByStorageKey("sk-001");
        assertThat(found).isPresent();
        assertThat(found.get().getIdempotencyKey()).isEqualTo("ik-001");
    }

    @Test
    void findByStorageKey_shouldReturnEmpty_whenNotFound() {
        Optional<IdempotencyRecord> found = repository.findByStorageKey("nonexistent");
        assertThat(found).isEmpty();
    }

    @Test
    void findByIdempotencyKey_shouldReturnRecord_whenExists() {
        repository.save(buildRecord("sk-002", "ik-002"));

        Optional<IdempotencyRecord> found = repository.findByIdempotencyKey("ik-002");
        assertThat(found).isPresent();
        assertThat(found.get().getStorageKey()).isEqualTo("sk-002");
    }

    @Test
    void findByIdempotencyKey_shouldReturnEmpty_whenNotFound() {
        Optional<IdempotencyRecord> found = repository.findByIdempotencyKey("missing-ik");
        assertThat(found).isEmpty();
    }

    @Test
    void deleteByCreatedAtBefore_shouldDeleteOldRecords() {
        repository.saveAndFlush(buildRecord("sk-003", "ik-003"));

        repository.deleteByCreatedAtBefore(Instant.now().plusSeconds(60));
        assertThat(repository.findByStorageKey("sk-003")).isEmpty();
    }

    @Test
    void deleteByCreatedAtBefore_shouldKeepFreshRecords() {
        repository.saveAndFlush(buildRecord("sk-004", "ik-004"));

        repository.deleteByCreatedAtBefore(Instant.now().minusSeconds(3600));
        assertThat(repository.findByStorageKey("sk-004")).isPresent();
    }

    @Test
    void save_shouldPersistRecordWithCreatedAt() {
        IdempotencyRecord saved = repository.save(buildRecord("sk-005", "ik-005"));
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    private IdempotencyRecord buildRecord(String storageKey, String idempotencyKey) {
        IdempotencyRecord record = new IdempotencyRecord();
        record.setStorageKey(storageKey);
        record.setIdempotencyKey(idempotencyKey);
        record.setCallerUserId("user-001");
        record.setOperationType("TRANSFER");
        record.setResponseStatus(200);
        record.setResponseBody("{\"status\":\"ok\"}");
        return record;
    }
}
