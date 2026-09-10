package com.group1.banking.repository;

import com.group1.banking.entity.PendingAgentActionEntity;
import com.group1.banking.entity.PendingAgentActionStatus;
import com.group1.banking.entity.PendingAgentActionType;
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
class PendingAgentActionRepositoryTest {

    @Autowired
    private PendingAgentActionRepository repository;

    private PendingAgentActionEntity build(String token) {
        PendingAgentActionEntity entity = new PendingAgentActionEntity();
        entity.setToken(token);
        entity.setCustomerId(42L);
        entity.setActionType(PendingAgentActionType.TRANSFER);
        entity.setParametersJson("{\"fromAccountId\":1,\"toAccountId\":2,\"amount\":50.00}");
        entity.setHumanSummary("Transfer $50.00 from Checking to Savings.");
        entity.setStatus(PendingAgentActionStatus.PENDING);
        entity.setExpiresAt(LocalDateTime.now().plusMinutes(5));
        return entity;
    }

    @Test
    void save_shouldPersistAndSetCreatedAt() {
        PendingAgentActionEntity saved = repository.save(build("token-001"));
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    void findById_shouldReturnEntity_whenTokenExists() {
        repository.save(build("token-002"));

        Optional<PendingAgentActionEntity> found = repository.findById("token-002");

        assertThat(found).isPresent();
        assertThat(found.get().getCustomerId()).isEqualTo(42L);
        assertThat(found.get().getActionType()).isEqualTo(PendingAgentActionType.TRANSFER);
        assertThat(found.get().getStatus()).isEqualTo(PendingAgentActionStatus.PENDING);
    }

    @Test
    void findById_shouldReturnEmpty_whenTokenDoesNotExist() {
        Optional<PendingAgentActionEntity> found = repository.findById("no-such-token");
        assertThat(found).isEmpty();
    }
}
