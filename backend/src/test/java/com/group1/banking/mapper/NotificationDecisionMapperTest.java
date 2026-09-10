package com.group1.banking.mapper;

import com.group1.banking.dto.response.NotificationDecisionResponse;
import com.group1.banking.entity.NotificationDecision;
import com.group1.banking.entity.NotificationDecisionEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationDecisionMapperTest {

    private NotificationDecisionMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new NotificationDecisionMapper();
    }

    @Test
    void toResponse_shouldMapEventId() {
        NotificationDecisionEntity entity = buildEntity();
        NotificationDecisionResponse response = mapper.toResponse(entity);
        assertThat(response.getEventId()).isEqualTo("evt-001");
    }

    @Test
    void toResponse_shouldMapDecisionAsString() {
        NotificationDecisionEntity entity = buildEntity();
        NotificationDecisionResponse response = mapper.toResponse(entity);
        assertThat(response.getDecision()).isEqualTo(NotificationDecision.RAISED.name());
    }

    @Test
    void toResponse_shouldMapDecisionReason() {
        NotificationDecisionEntity entity = buildEntity();
        NotificationDecisionResponse response = mapper.toResponse(entity);
        assertThat(response.getDecisionReason()).isEqualTo("Threshold exceeded");
    }

    @Test
    void toResponse_shouldMapCustomerId() {
        NotificationDecisionEntity entity = buildEntity();
        NotificationDecisionResponse response = mapper.toResponse(entity);
        assertThat(response.getCustomerId()).isEqualTo(42L);
    }

    @Test
    void toResponse_shouldMapAccountId() {
        NotificationDecisionEntity entity = buildEntity();
        NotificationDecisionResponse response = mapper.toResponse(entity);
        assertThat(response.getAccountId()).isEqualTo(1001L);
    }

    @Test
    void toResponse_shouldMapMandatoryOverrideTrue() {
        NotificationDecisionEntity entity = buildEntity();
        entity.setMandatoryOverride(true);
        NotificationDecisionResponse response = mapper.toResponse(entity);
        assertThat(response.isMandatoryOverride()).isTrue();
    }

    @Test
    void toResponse_shouldMapMandatoryOverrideFalse() {
        NotificationDecisionEntity entity = buildEntity();
        entity.setMandatoryOverride(false);
        NotificationDecisionResponse response = mapper.toResponse(entity);
        assertThat(response.isMandatoryOverride()).isFalse();
    }

    @Test
    void toResponse_shouldMapEvaluatedAt() {
        NotificationDecisionEntity entity = buildEntity();
        LocalDateTime now = LocalDateTime.now();
        entity.setEvaluatedAt(now);
        NotificationDecisionResponse response = mapper.toResponse(entity);
        assertThat(response.getEvaluatedAt()).isEqualTo(now);
    }

    @Test
    void toResponse_shouldMapSuppressedDecision() {
        NotificationDecisionEntity entity = buildEntity();
        entity.setDecision(NotificationDecision.SUPPRESSED);
        NotificationDecisionResponse response = mapper.toResponse(entity);
        assertThat(response.getDecision()).isEqualTo("SUPPRESSED");
    }

    private NotificationDecisionEntity buildEntity() {
        NotificationDecisionEntity entity = new NotificationDecisionEntity();
        entity.setEventId("evt-001");
        entity.setEventType("LARGE_DEBIT");
        entity.setAccountId(1001L);
        entity.setCustomerId(42L);
        entity.setBusinessTimestamp(LocalDateTime.now());
        entity.setDecision(NotificationDecision.RAISED);
        entity.setDecisionReason("Threshold exceeded");
        entity.setMandatoryOverride(false);
        entity.setEvaluatedAt(LocalDateTime.now());
        return entity;
    }
}
