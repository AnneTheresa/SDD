package com.group1.banking.controller;

import com.group1.banking.dto.customer.OperationResult;
import com.group1.banking.dto.customer.TransferRequest;
import com.group1.banking.entity.PendingAgentActionEntity;
import com.group1.banking.exception.PermissionDeniedException;
import com.group1.banking.security.CustomUserPrincipal;
import com.group1.banking.service.ConfirmationGateService;
import com.group1.banking.service.impl.MonetaryOperationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Executes an agent-proposed action once the customer has explicitly confirmed it
 * (AC4). This is the ONLY path by which a proposal from {@code TransferChatTool} (or
 * any future mutating chat tool) can actually take effect - the model's tool-calling
 * loop itself has no way to reach this endpoint.
 */
@RestController
@RequestMapping("/api/chat/confirmations")
@PreAuthorize("isAuthenticated()")
public class AgentActionConfirmationController {

    private static final String DEFAULT_ACTOR_ROLE = "UNKNOWN";
    private static final String ROLE_PREFIX = "ROLE_";

    private final ConfirmationGateService confirmationGateService;
    private final MonetaryOperationService monetaryOperationService;
    private final JsonMapper objectMapper;

    public AgentActionConfirmationController(ConfirmationGateService confirmationGateService,
                                              MonetaryOperationService monetaryOperationService,
                                              JsonMapper objectMapper) {
        this.confirmationGateService = confirmationGateService;
        this.monetaryOperationService = monetaryOperationService;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/{token}")
    public ResponseEntity<Object> confirm(@PathVariable String token,
                                           @AuthenticationPrincipal CustomUserPrincipal principal) {
        CustomUserPrincipal caller = extractPrincipal(principal);
        String actorRole = extractRole(caller);

        PendingAgentActionEntity resolved =
                confirmationGateService.confirmAndConsume(token, caller.getCustomerId(), actorRole);

        OperationResult result = switch (resolved.getActionType()) {
            case TRANSFER -> executeTransfer(resolved);
        };

        return ResponseEntity.status(result.status()).body(result.body());
    }

    private OperationResult executeTransfer(PendingAgentActionEntity resolved) {
        TransferRequest request;
        try {
            request = objectMapper.readValue(resolved.getParametersJson(), TransferRequest.class);
        } catch (JacksonException ex) {
            throw new IllegalStateException(
                    "Failed to deserialize stored agent action parameters for token " + resolved.getToken(), ex);
        }
        return monetaryOperationService.transfer(request, resolved.getToken());
    }

    private CustomUserPrincipal extractPrincipal(CustomUserPrincipal principal) {
        if (principal == null) {
            throw new PermissionDeniedException("AUTHENTICATION");
        }
        return principal;
    }

    private String extractRole(CustomUserPrincipal principal) {
        return principal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority -> authority.startsWith(ROLE_PREFIX))
                .map(authority -> authority.substring(ROLE_PREFIX.length()))
                .findFirst()
                .orElse(DEFAULT_ACTOR_ROLE);
    }
}
