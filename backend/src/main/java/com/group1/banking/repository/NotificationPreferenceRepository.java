package com.group1.banking.repository;

import com.group1.banking.entity.NotificationPreferenceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationPreferenceRepository extends JpaRepository<NotificationPreferenceEntity, Long> {
    Optional<NotificationPreferenceEntity> findByCustomerIdAndEventType(Long customerId, String eventType);
    List<NotificationPreferenceEntity> findAllByCustomerId(Long customerId);
}
