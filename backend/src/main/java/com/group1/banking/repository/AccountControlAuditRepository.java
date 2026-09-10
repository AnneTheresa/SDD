package com.group1.banking.repository;

import com.group1.banking.entity.accountcontrol.AccountControlAuditEvent;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountControlAuditRepository extends JpaRepository<AccountControlAuditEvent, Long> {
    List<AccountControlAuditEvent> findAllByAccountIdOrderByCreatedAtDesc(Long accountId);
}
