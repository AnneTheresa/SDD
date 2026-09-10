package com.group1.banking.controller;

import com.group1.banking.dto.request.RecategoriseRequest;
import com.group1.banking.dto.response.RecategoriseResponse;
import com.group1.banking.dto.response.SpendingInsightResponse;
import com.group1.banking.exception.PermissionDeniedException;
import com.group1.banking.security.CustomUserPrincipal;
import com.group1.banking.service.SpendingInsightService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Spending insight controller. (T099)
 */
@RestController
@RequestMapping("/accounts/{accountId}")
public class InsightController {

    private final SpendingInsightService spendingInsightService;

    public InsightController(SpendingInsightService spendingInsightService) {
        this.spendingInsightService = spendingInsightService;
    }

    @GetMapping("/insights")
    public ResponseEntity<SpendingInsightResponse> getInsights(
            @PathVariable long accountId,
            @RequestParam int year,
            @RequestParam int month,
            @AuthenticationPrincipal CustomUserPrincipal principal) {
        return ResponseEntity.ok(spendingInsightService.getInsights(accountId, year, month, extractPrincipal(principal)));
    }

    @PutMapping("/transactions/{transactionId}/category")
    public ResponseEntity<RecategoriseResponse> recategorise(
            @PathVariable long accountId,
            @PathVariable String transactionId,
            @RequestBody @Valid RecategoriseRequest request,
            @AuthenticationPrincipal CustomUserPrincipal principal) {
        return ResponseEntity.ok(spendingInsightService.recategorise(
            accountId, transactionId, request.getCategory(), extractPrincipal(principal)));
    }

    private CustomUserPrincipal extractPrincipal(CustomUserPrincipal principal) {
        if (principal == null) {
            throw new PermissionDeniedException("AUTHENTICATION");
        }
        return principal;
    }
}
