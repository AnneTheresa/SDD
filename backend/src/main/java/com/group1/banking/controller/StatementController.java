package com.group1.banking.controller;

import com.group1.banking.exception.PermissionDeniedException;
import com.group1.banking.security.CustomUserPrincipal;
import com.group1.banking.service.MonthlyStatementService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Statement controller. (T083)
 */
@RestController
@RequestMapping("/accounts/{accountId}/statements")
public class StatementController {

    private final MonthlyStatementService monthlyStatementService;

    public StatementController(MonthlyStatementService monthlyStatementService) {
        this.monthlyStatementService = monthlyStatementService;
    }

    @GetMapping("/{period}")
    public ResponseEntity<byte[]> getStatement(
            @PathVariable long accountId,
            @PathVariable String period,
            @AuthenticationPrincipal CustomUserPrincipal principal) {
        byte[] pdfBytes = monthlyStatementService.generateStatement(accountId, period, extractPrincipal(principal));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData(
                "attachment",
                "statement-" + accountId + "-" + period + ".pdf");
        return ResponseEntity.ok()
                .headers(headers)
                .body(pdfBytes);
    }

    private CustomUserPrincipal extractPrincipal(CustomUserPrincipal principal) {
        if (principal == null) {
            throw new PermissionDeniedException("AUTHENTICATION");
        }
        return principal;
    }
}
