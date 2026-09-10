package com.group1.banking.repository;

import com.group1.banking.entity.Frequency;
import com.group1.banking.entity.StandingOrderEntity;
import com.group1.banking.entity.StandingOrderStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Repository tests for StandingOrderRepository using @DataJpaTest.
 */
@DataJpaTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class StandingOrderRepositoryTest {

    @Autowired
    private StandingOrderRepository standingOrderRepository;

    private StandingOrderEntity activeOrder;
    private StandingOrderEntity retryOrder;

    @BeforeEach
    void setUp() {
        LocalDateTime now = LocalDateTime.now();

        activeOrder = new StandingOrderEntity();
        activeOrder.setStandingOrderId(UUID.randomUUID().toString());
        activeOrder.setSourceAccountId(1001L);
        activeOrder.setPayeeAccount(9999L);
        activeOrder.setPayeeName("Landlord");
        activeOrder.setAmount(new BigDecimal("500.00"));
        activeOrder.setFrequency(Frequency.MONTHLY);
        activeOrder.setStatus(StandingOrderStatus.ACTIVE);
        activeOrder.setStartDate(now.minusDays(30));
        activeOrder.setNextRunDate(now.minusMinutes(10)); // past → due
        activeOrder.setReference("RENT001");
        standingOrderRepository.save(activeOrder);

        retryOrder = new StandingOrderEntity();
        retryOrder.setStandingOrderId(UUID.randomUUID().toString());
        retryOrder.setSourceAccountId(1001L);
        retryOrder.setPayeeAccount(8888L);
        retryOrder.setPayeeName("Insurance");
        retryOrder.setAmount(new BigDecimal("150.00"));
        retryOrder.setFrequency(Frequency.MONTHLY);
        retryOrder.setStatus(StandingOrderStatus.RETRY_PENDING);
        retryOrder.setStartDate(now.minusDays(30));
        retryOrder.setNextRunDate(now.minusMinutes(5));
        retryOrder.setReference("INS001");
        standingOrderRepository.save(retryOrder);
    }

    @Test
    void findBySourceAccountId_shouldReturnAllOrdersForAccount() {
        List<StandingOrderEntity> orders = standingOrderRepository.findBySourceAccountId(1001L);
        assertThat(orders).hasSize(2);
    }

    @Test
    void findBySourceAccountId_shouldReturnEmpty_whenNoOrders() {
        List<StandingOrderEntity> orders = standingOrderRepository.findBySourceAccountId(9999L);
        assertThat(orders).isEmpty();
    }

    @Test
    void findByStatusAndNextRunDateBefore_shouldReturnDueActiveOrders() {
        List<StandingOrderEntity> orders = standingOrderRepository.findByStatusAndNextRunDateBefore(
                StandingOrderStatus.ACTIVE, LocalDateTime.now());

        assertThat(orders).hasSize(1);
        assertThat(orders.get(0).getPayeeName()).isEqualTo("Landlord");
    }

    @Test
    void findByStatusAndNextRunDateBefore_shouldReturnDueRetryOrders() {
        List<StandingOrderEntity> orders = standingOrderRepository.findByStatusAndNextRunDateBefore(
                StandingOrderStatus.RETRY_PENDING, LocalDateTime.now());

        assertThat(orders).hasSize(1);
        assertThat(orders.get(0).getPayeeName()).isEqualTo("Insurance");
    }

    @Test
    void findByStatusAndNextRunDateBefore_shouldReturnEmpty_whenNotDue() {
        List<StandingOrderEntity> orders = standingOrderRepository.findByStatusAndNextRunDateBefore(
                StandingOrderStatus.ACTIVE, LocalDateTime.now().minusDays(1)); // yesterday cutoff

        assertThat(orders).isEmpty();
    }

    @Test
    void findByStatus_shouldReturnOrdersByStatus() {
        List<StandingOrderEntity> active = standingOrderRepository.findByStatus(StandingOrderStatus.ACTIVE);
        assertThat(active).hasSize(1);

        List<StandingOrderEntity> retry = standingOrderRepository.findByStatus(StandingOrderStatus.RETRY_PENDING);
        assertThat(retry).hasSize(1);
    }

    @Test
    void findBySourceAccountIdAndPayeeAccountAndAmountAndFrequencyAndStatus_shouldFindDuplicate() {
        Optional<StandingOrderEntity> dup = standingOrderRepository
                .findBySourceAccountIdAndPayeeAccountAndAmountAndFrequencyAndStatus(
                        1001L, 9999L, new BigDecimal("500.00"), Frequency.MONTHLY, StandingOrderStatus.ACTIVE);

        assertThat(dup).isPresent();
        assertThat(dup.get().getReference()).isEqualTo("RENT001");
    }

    @Test
    void findBySourceAccountIdAndPayeeAccountAndAmountAndFrequencyAndStatus_shouldReturnEmpty_whenNoDuplicate() {
        Optional<StandingOrderEntity> dup = standingOrderRepository
                .findBySourceAccountIdAndPayeeAccountAndAmountAndFrequencyAndStatus(
                        1001L, 9999L, new BigDecimal("999.00"), Frequency.MONTHLY, StandingOrderStatus.ACTIVE);

        assertThat(dup).isEmpty();
    }

    @Test
    void findById_shouldReturnOrder_whenExists() {
        Optional<StandingOrderEntity> found = standingOrderRepository.findById(activeOrder.getStandingOrderId());
        assertThat(found).isPresent();
        assertThat(found.get().getAmount()).isEqualByComparingTo(new BigDecimal("500.00"));
    }

    @Test
    void save_shouldUpdateStatus() {
        activeOrder.setStatus(StandingOrderStatus.CANCELLED);
        standingOrderRepository.save(activeOrder);

        Optional<StandingOrderEntity> found = standingOrderRepository.findById(activeOrder.getStandingOrderId());
        assertThat(found.get().getStatus()).isEqualTo(StandingOrderStatus.CANCELLED);
    }
}
