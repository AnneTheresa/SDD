package com.group1.banking.dto.accountcontrol;

import jakarta.validation.constraints.NotBlank;

public record FreezeAccountRequest(
        @NotBlank(message = "reason is required") String reason,
        String reasonCode,
        String notes
) {
}
