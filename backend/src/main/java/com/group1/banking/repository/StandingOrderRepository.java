package com.group1.banking.repository;

import com.group1.banking.entity.StandingOrderEntity;
import com.group1.banking.entity.StandingOrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.math.BigDecimal;
import com.group1.banking.entity.Frequency;
import java.util.Optional;

@Repository
public interface StandingOrderRepository extends JpaRepository<StandingOrderEntity, String> {

    List<StandingOrderEntity> findBySourceAccountId(Long sourceAccountId);

    List<StandingOrderEntity> findByStatusAndNextRunDateBefore(StandingOrderStatus status, LocalDateTime cutoff);

    Optional<StandingOrderEntity> findBySourceAccountIdAndPayeeAccountAndAmountAndFrequencyAndStatus(
            Long sourceAccountId, Long payeeAccount, BigDecimal amount,
            Frequency frequency, StandingOrderStatus status);

    List<StandingOrderEntity> findByStatus(StandingOrderStatus status);
}
