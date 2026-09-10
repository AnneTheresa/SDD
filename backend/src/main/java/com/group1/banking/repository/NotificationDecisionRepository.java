package com.group1.banking.repository;

import com.group1.banking.entity.NotificationDecisionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationDecisionRepository extends JpaRepository<NotificationDecisionEntity, String> {
    boolean existsByEventId(String eventId);
}
