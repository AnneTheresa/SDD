package com.group1.banking.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    @Test
    void handleGone_shouldReturn410WithCodeAndMessage() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        GoneException ex = new GoneException("CONFIRMATION_EXPIRED", "This confirmation has expired.", null);

        ResponseEntity<com.group1.banking.dto.common.ErrorResponse> response = handler.handleGone(ex);

        assertThat(response.getStatusCode().value()).isEqualTo(410);
        assertThat(response.getBody().getCode()).isEqualTo("CONFIRMATION_EXPIRED");
        assertThat(response.getBody().getMessage()).isEqualTo("This confirmation has expired.");
    }
}
