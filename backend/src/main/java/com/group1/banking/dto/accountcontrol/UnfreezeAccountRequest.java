package com.group1.banking.dto.accountcontrol;

public record UnfreezeAccountRequest(
        String reason,
        String notes
) {
}
