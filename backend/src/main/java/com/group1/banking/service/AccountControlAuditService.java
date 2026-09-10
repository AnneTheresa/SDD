package com.group1.banking.service;

import com.group1.banking.entity.AccountStatus;
import com.group1.banking.entity.accountcontrol.AccountControlActionType;
import com.group1.banking.entity.accountcontrol.AccountControlAuditEvent;
import com.group1.banking.repository.AccountControlAuditRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class AccountControlAuditService {

    private final AccountControlAuditRepository accountControlAuditRepository;

    public AccountControlAuditService(AccountControlAuditRepository accountControlAuditRepository) {
        this.accountControlAuditRepository = accountControlAuditRepository;
    }

    public AccountControlAuditEvent logEvent(Long accountId,
                                             String adminUserId,
                                             String adminRole,
                                             AccountControlActionType actionType,
                                             AccountStatus previousStatus,
                                             AccountStatus newStatus,
                                             String reason,
                                             String notes) {
        AccountControlAuditEvent event = new AccountControlAuditEvent();
        event.setAccountId(accountId);
        event.setAdminUserId(adminUserId);
        event.setAdminRole(adminRole);
        event.setActionType(actionType);
        event.setPreviousStatus(previousStatus);
        event.setNewStatus(newStatus);
        event.setReason(reason);
        event.setNotes(notes);
        event.setCreatedAt(Instant.now());
        return accountControlAuditRepository.save(event);
    }

    public List<AccountControlAuditEvent> getHistory(Long accountId) {
        return accountControlAuditRepository.findAllByAccountIdOrderByCreatedAtDesc(accountId);
    }
}
