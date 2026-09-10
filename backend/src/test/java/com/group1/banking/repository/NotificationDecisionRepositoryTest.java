package com.group1.banking.repository;

import com.group1.banking.entity.NotificationDecision;
import com.group1.banking.entity.NotificationDecisionEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class NotificationDecisionRepositoryTest {

    @Autowired
    private NotificationDecisionRepository repository;

    @Test
    void existsByEventId_shouldReturnTrue_whenEventExists() {
        repository.save(buildEntity("evt-001"));
        assertThat(repository.existsByEventId("evt-001")).isTrue();
    }

    @Test
    void existsByEventId_shouldReturnFalse_whenEventDoesNotExist() {
        assertThat(repository.existsByEventId("nonexistent")).isFalse();
    }

    @Test
    void existsByEventId_shouldReturnFalse_afterDeletion() {
        repository.save(buildEntity("evt-002"));
        repository.deleteAll();
        assertThat(repository.existsByEventId("evt-002")).isFalse();
    }

    @Test
    void save_shouldPersistWithEvaluatedAt() {
        NotificationDecisionEntity saved = repository.save(buildEntity("evt-003"));
        assertThat(saved.getEvaluatedAt()).isNotNull();
    }

    @Test
    void findById_shouldReturnEntity_whenExists() {
        repository.save(buildEntity("evt-004"));
        assertThat(repository.findById("evt-004")).isPresent();
    }

    private NotificationDecisionEntity buildEntity(String eventId) {
        NotificationDecisionEntity entity = new NotificationDecisionEntity();
        entity.setEventId(eventId);
        entity.setEventType("LARGE_DEBIT");
        entity.setAccountId(1001L);
        entity.setCustomerId(42L);
        entity.setBusinessTimestamp(LocalDateTime.now());
        entity.setDecision(NotificationDecision.RAISED);
        entity.setDecisionReason("Threshold exceeded");
        entity.setMandatoryOverride(false);
        return entity;
    }
}
