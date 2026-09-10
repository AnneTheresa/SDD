package com.group1.banking.dto.accountcontrol;

import com.group1.banking.entity.AccountStatus;
import com.group1.banking.entity.accountcontrol.AccountControlActionType;
import java.time.Instant;

public record AccountControlActionResponse(
        Long accountId,
        AccountStatus previousStatus,
        AccountStatus newStatus,
        AccountControlActionType actionType,
        Instant timestamp
) {
}
