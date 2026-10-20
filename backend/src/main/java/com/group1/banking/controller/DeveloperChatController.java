package com.group1.banking.controller;

import com.group1.banking.dto.chat.DeveloperChatRequest;
import com.group1.banking.dto.chat.DeveloperChatResponse;
import com.group1.banking.exception.ForbiddenException;
import com.group1.banking.security.CustomUserPrincipal;
import com.group1.banking.service.impl.DeveloperChatService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/developer/chat")
@PreAuthorize("hasRole('DEVELOPER')")
public class DeveloperChatController {

    private final DeveloperChatService developerChatService;

    public DeveloperChatController(DeveloperChatService developerChatService) {
        this.developerChatService = developerChatService;
    }

    @PostMapping
    public ResponseEntity<DeveloperChatResponse> ask(
            @RequestBody @Valid DeveloperChatRequest request,
            @AuthenticationPrincipal CustomUserPrincipal principal) {
        if (principal == null || principal.getAuthorities().stream()
                .noneMatch(authority -> "ROLE_DEVELOPER".equals(authority.getAuthority()))) {
            throw new ForbiddenException("ROLE_NOT_ALLOWED",
                    "You are not authorized to use the developer assistant.");
        }
        return ResponseEntity.ok(developerChatService.ask(principal.getUserId(), request));
    }
}
