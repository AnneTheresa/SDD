package com.group1.banking.repository;

import com.group1.banking.entity.PendingAgentActionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PendingAgentActionRepository extends JpaRepository<PendingAgentActionEntity, String> {
}
