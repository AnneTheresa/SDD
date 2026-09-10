package com.group1.banking.mapper;

import com.group1.banking.dto.response.StandingOrderResponse;
import com.group1.banking.entity.Frequency;
import com.group1.banking.entity.StandingOrderEntity;
import com.group1.banking.entity.StandingOrderStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class StandingOrderMapperTest {

    private StandingOrderMapper standingOrderMapper;

    @BeforeEach
    void setUp() {
        standingOrderMapper = new StandingOrderMapper();
    }

    @Test
    void toResponse_shouldMapStandingOrderId() {
        StandingOrderEntity entity = buildEntity();
        StandingOrderResponse response = standingOrderMapper.toResponse(entity);
        assertThat(response.getStandingOrderId()).isEqualTo("so-001");
    }

    @Test
    void toResponse_shouldMapSourceAccountId() {
        StandingOrderEntity entity = buildEntity();
        StandingOrderResponse response = standingOrderMapper.toResponse(entity);
        assertThat(response.getSourceAccountId()).isEqualTo(1001L);
    }

    @Test
    void toResponse_shouldMapAmount() {
        StandingOrderEntity entity = buildEntity();
        StandingOrderResponse response = standingOrderMapper.toResponse(entity);
        assertThat(response.getAmount()).isEqualByComparingTo(new BigDecimal("150.00"));
    }

    @Test
    void toResponse_shouldMapFrequencyAsString() {
        StandingOrderEntity entity = buildEntity();
        StandingOrderResponse response = standingOrderMapper.toResponse(entity);
        assertThat(response.getFrequency()).isEqualTo(Frequency.MONTHLY.name());
    }

    @Test
    void toResponse_shouldMapStatusAsString() {
        StandingOrderEntity entity = buildEntity();
        StandingOrderResponse response = standingOrderMapper.toResponse(entity);
        assertThat(response.getStatus()).isEqualTo(StandingOrderStatus.ACTIVE.name());
    }

    @Test
    void toResponse_shouldMapPayeeName() {
        StandingOrderEntity entity = buildEntity();
        StandingOrderResponse response = standingOrderMapper.toResponse(entity);
        assertThat(response.getPayeeName()).isEqualTo("Bob Jones");
    }

    @Test
    void toResponse_shouldMapReference() {
        StandingOrderEntity entity = buildEntity();
        StandingOrderResponse response = standingOrderMapper.toResponse(entity);
        assertThat(response.getReference()).isEqualTo("RENT-JAN");
    }

    @Test
    void toResponse_shouldMapPayeeAccount() {
        StandingOrderEntity entity = buildEntity();
        StandingOrderResponse response = standingOrderMapper.toResponse(entity);
        assertThat(response.getPayeeAccount()).isEqualTo(9999L);
    }

    private StandingOrderEntity buildEntity() {
        StandingOrderEntity entity = new StandingOrderEntity();
        entity.setStandingOrderId("so-001");
        entity.setSourceAccountId(1001L);
        entity.setPayeeAccount(9999L);
        entity.setPayeeName("Bob Jones");
        entity.setAmount(new BigDecimal("150.00"));
        entity.setFrequency(Frequency.MONTHLY);
        entity.setStartDate(LocalDateTime.now().minusMonths(1));
        entity.setReference("RENT-JAN");
        entity.setStatus(StandingOrderStatus.ACTIVE);
        entity.setNextRunDate(LocalDateTime.now().plusMonths(1));
        return entity;
    }
}
