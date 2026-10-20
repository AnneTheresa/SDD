package com.group1.banking.controller;

import com.group1.banking.dto.auth.AuthResponse;
import com.group1.banking.dto.auth.LoginRequest;
import com.group1.banking.dto.auth.ProvisionAccountRequest;
import com.group1.banking.dto.auth.RegisterRequest;
import com.group1.banking.dto.auth.UserResponse;
import com.group1.banking.enums.RoleName;
import com.group1.banking.exception.ForbiddenException;
import com.group1.banking.security.CustomUserPrincipal;
import com.group1.banking.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@RequestBody @Valid RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/provision")
    @PreAuthorize("hasAnyRole('DEVELOPER', 'BANK_ADMINISTRATOR')")
    public ResponseEntity<UserResponse> provisionAccount(
            @RequestBody @Valid ProvisionAccountRequest request,
            @AuthenticationPrincipal CustomUserPrincipal principal) {
        if (principal == null) {
            throw new ForbiddenException("ROLE_NOT_ALLOWED",
                    "You are not authorized to provision accounts.");
        }
        boolean bankAdministrator = principal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_BANK_ADMINISTRATOR"::equals);
        boolean developer = principal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_DEVELOPER"::equals);
        if (!bankAdministrator && !developer) {
            throw new ForbiddenException("ROLE_NOT_ALLOWED",
                    "You are not authorized to provision accounts.");
        }
        RoleName actorRole = bankAdministrator ? RoleName.BANK_ADMINISTRATOR : RoleName.DEVELOPER;
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(authService.provisionAccount(request, actorRole));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody @Valid LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }
}
