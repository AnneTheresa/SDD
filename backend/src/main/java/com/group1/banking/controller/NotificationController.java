package com.group1.banking.controller;

import com.group1.banking.dto.request.NotificationEventRequest;
import com.group1.banking.dto.response.NotificationDecisionResponse;
import com.group1.banking.service.NotificationEvaluationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Notification evaluation controller. (T075)
 * Security context populated by ServiceApiKeyFilter (order 1 security chain).
 */
@RestController
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationEvaluationService notificationEvaluationService;

    public NotificationController(NotificationEvaluationService notificationEvaluationService) {
        this.notificationEvaluationService = notificationEvaluationService;
    }

    @PostMapping("/evaluate")
    public ResponseEntity<NotificationDecisionResponse> evaluate(
            @RequestBody @Valid NotificationEventRequest request) {
        return ResponseEntity.ok(notificationEvaluationService.evaluate(request));
    }
}
