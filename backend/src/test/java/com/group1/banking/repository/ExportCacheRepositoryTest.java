package com.group1.banking.repository;

import com.group1.banking.entity.ExportCacheEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class ExportCacheRepositoryTest {

    @Autowired
    private ExportCacheRepository repository;

    @Test
    void findByAccountIdAndParamHash_shouldReturnEntity_whenExists() {
        repository.save(buildEntity(1001L, "hash-abc"));

        Optional<ExportCacheEntity> found = repository.findByAccountIdAndParamHash(1001L, "hash-abc");
        assertThat(found).isPresent();
        assertThat(found.get().getPdfData()).isEqualTo(new byte[]{1, 2, 3});
    }

    @Test
    void findByAccountIdAndParamHash_shouldReturnEmpty_whenNotFound() {
        Optional<ExportCacheEntity> found = repository.findByAccountIdAndParamHash(9999L, "missing");
        assertThat(found).isEmpty();
    }

    @Test
    void findByAccountIdAndParamHash_shouldReturnEmpty_whenDifferentAccount() {
        repository.save(buildEntity(1001L, "hash-xyz"));

        Optional<ExportCacheEntity> found = repository.findByAccountIdAndParamHash(2002L, "hash-xyz");
        assertThat(found).isEmpty();
    }

    @Test
    void deleteByCreatedAtBefore_shouldDeleteOldEntries() {
        repository.saveAndFlush(buildEntity(1001L, "hash-old"));

        repository.deleteByCreatedAtBefore(LocalDateTime.now().plusMinutes(1));
        assertThat(repository.findByAccountIdAndParamHash(1001L, "hash-old")).isEmpty();
    }

    @Test
    void deleteByCreatedAtBefore_shouldKeepRecentEntries() {
        repository.saveAndFlush(buildEntity(1001L, "hash-recent"));

        repository.deleteByCreatedAtBefore(LocalDateTime.now().minusHours(1));
        assertThat(repository.findByAccountIdAndParamHash(1001L, "hash-recent")).isPresent();
    }

    @Test
    void save_shouldAutoAssignCacheId() {
        ExportCacheEntity saved = repository.save(buildEntity(1001L, "hash-id"));
        assertThat(saved.getCacheId()).isNotNull();
    }

    private ExportCacheEntity buildEntity(Long accountId, String paramHash) {
        ExportCacheEntity entity = new ExportCacheEntity();
        entity.setAccountId(accountId);
        entity.setParamHash(paramHash);
        entity.setPdfData(new byte[]{1, 2, 3});
        return entity;
    }
}
