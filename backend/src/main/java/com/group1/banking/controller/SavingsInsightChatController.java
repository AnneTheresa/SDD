package com.group1.banking.controller;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.group1.banking.dto.chat.ChatQueryRequest;
import com.group1.banking.dto.chat.ChatQueryResponse;
import com.group1.banking.exception.PermissionDeniedException;
import com.group1.banking.security.CustomUserPrincipal;
import com.group1.banking.service.impl.SavingsInsightChatService;

/**
 * Savings Insight Chatbot (RAG-based). See BRD 6.1: a conversational endpoint that
 * answers savings/spending/financial-wellness questions using the customer's own
 * transaction and savings goal context plus an approved knowledge base, with
 * guardrails against out-of-scope advice.
 */
@RestController
@RequestMapping("/api/chat")
@PreAuthorize("isAuthenticated()")
public class SavingsInsightChatController {

    private static final String DEFAULT_ACTOR_ROLE = "UNKNOWN";
    private static final String ROLE_PREFIX = "ROLE_";

    private final SavingsInsightChatService savingsInsightChatService;

    public SavingsInsightChatController(SavingsInsightChatService savingsInsightChatService) {
        this.savingsInsightChatService = savingsInsightChatService;
    }

    @PostMapping("/savings-insights")
    public ResponseEntity<ChatQueryResponse> askSavingsInsight(
            @RequestBody @Valid ChatQueryRequest request,
            @AuthenticationPrincipal CustomUserPrincipal principal) {
        CustomUserPrincipal caller = extractPrincipal(principal);
        String actorRole = extractRole(caller);
        ChatQueryResponse response =
                savingsInsightChatService.ask(caller.getCustomerId(), actorRole, request.getMessage());
        return ResponseEntity.ok(response);
    }

    private CustomUserPrincipal extractPrincipal(CustomUserPrincipal principal) {
        if (principal == null) {
            throw new PermissionDeniedException("AUTHENTICATION");
        }
        return principal;
    }

    // Mirrors the "first role name" convention already used elsewhere (e.g.
    // GicService, via user.getRoles()) for the shared audit trail's actorRole -
    // strips the "ROLE_" prefix Spring Security adds so the stored value reads the
    // same as audit rows written from other services.
    private String extractRole(CustomUserPrincipal principal) {
        return principal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority -> authority.startsWith(ROLE_PREFIX))
                .map(authority -> authority.substring(ROLE_PREFIX.length()))
                .findFirst()
                .orElse(DEFAULT_ACTOR_ROLE);
    }
}
