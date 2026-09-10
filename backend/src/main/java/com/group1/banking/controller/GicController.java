package com.group1.banking.controller;

import com.group1.banking.dto.gic.CreateGicRequest;
import com.group1.banking.dto.gic.GicResponse;
import com.group1.banking.dto.gic.RedeemGicResponse;
import com.group1.banking.service.impl.GicService;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST endpoints for GIC investment management on RRSP accounts.
 *
 * POST   /accounts/{accountId}/gic                    — create a new GIC for an RRSP account
 * GET    /accounts/{accountId}/gic                    — list all GICs for an RRSP account
 * POST   /accounts/{accountId}/gic/{gicId}/redeem     — redeem a specific GIC by ID
 */
@RestController
@RequestMapping("/accounts/{accountId}/gic")
public class GicController {

    private final GicService gicService;

    public GicController(GicService gicService) {
        this.gicService = gicService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GicResponse createGic(
            @PathVariable Long accountId,
            @Valid @RequestBody CreateGicRequest request) {
        return gicService.createGic(accountId, request);
    }

    @GetMapping
    public List<GicResponse> getGics(@PathVariable Long accountId) {
        return gicService.getGics(accountId);
    }

    @PostMapping("/{gicId}/redeem")
    public RedeemGicResponse redeemGic(
            @PathVariable Long accountId,
            @PathVariable String gicId) {
        return gicService.redeemGic(accountId, gicId);
    }
}
