package com.group1.banking.dto.accountcontrol;

import com.group1.banking.entity.AccountStatus;
import com.group1.banking.entity.accountcontrol.AccountControlActionType;
import com.group1.banking.entity.accountcontrol.AccountControlAuditEvent;
import java.time.Instant;

public record AccountControlHistoryEventResponse(
        Long eventId,
        AccountControlActionType actionType,
        AccountStatus previousStatus,
        AccountStatus newStatus,
        String adminUserId,
        String adminRole,
        String reason,
        String notes,
        Instant timestamp
) {
    public static AccountControlHistoryEventResponse from(AccountControlAuditEvent event) {
        return new AccountControlHistoryEventResponse(
                event.getEventId(),
                event.getActionType(),
                event.getPreviousStatus(),
                event.getNewStatus(),
                event.getAdminUserId(),
                event.getAdminRole(),
                event.getReason(),
                event.getNotes(),
                event.getCreatedAt());
    }
}
