package com.group1.banking.dto.accountcontrol;

import java.util.List;

public record AccountControlHistoryResponse(Long accountId, List<AccountControlHistoryEventResponse> events) {
}
