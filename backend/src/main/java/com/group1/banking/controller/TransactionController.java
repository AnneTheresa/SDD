package com.group1.banking.controller;

import com.group1.banking.dto.response.TransactionHistoryResponse;
import com.group1.banking.exception.PermissionDeniedException;
import com.group1.banking.security.CustomUserPrincipal;
import com.group1.banking.security.UserPrincipal;
import com.group1.banking.service.TransactionHistoryService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Transaction history controller. (T029)
 */
@RestController
@RequestMapping("/accounts/{accountId}/transactions")
public class TransactionController {

    private final TransactionHistoryService transactionHistoryService;

    public TransactionController(TransactionHistoryService transactionHistoryService) {
        this.transactionHistoryService = transactionHistoryService;
    }

    @GetMapping
    public ResponseEntity<TransactionHistoryResponse> getTransactionHistory(
            @PathVariable long accountId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @AuthenticationPrincipal CustomUserPrincipal principal) {
        UserPrincipal caller = extractPrincipal(principal);
        TransactionHistoryResponse response = transactionHistoryService.getHistory(
                accountId, startDate, endDate, caller);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportTransactionsAsPdf(
            @PathVariable long accountId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @AuthenticationPrincipal CustomUserPrincipal principal) {
        UserPrincipal caller = extractPrincipal(principal);
        byte[] pdfBytes = transactionHistoryService.exportPdf(accountId, startDate, endDate, caller);

        String filename = "statement-" + accountId + "-"
                + (startDate != null ? startDate : "default") + "-"
                + (endDate != null ? endDate : "default") + ".pdf";

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(pdfBytes);
    }

    private UserPrincipal extractPrincipal(CustomUserPrincipal principal) {
        if (principal == null) {
            throw new PermissionDeniedException("AUTHENTICATION");
        }
        List<String> roles = principal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a.startsWith("ROLE_"))
                .map(a -> a.substring(5))
                .toList();
        List<String> permissions = principal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> !a.startsWith("ROLE_"))
                .toList();
        return new UserPrincipal(
                principal.getUserId().toString(),
                principal.getName(),
                roles,
                permissions,
                principal.getCustomerId());
    }
}
