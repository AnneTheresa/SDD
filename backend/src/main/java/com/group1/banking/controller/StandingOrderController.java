package com.group1.banking.controller;
 
import com.group1.banking.dto.request.CreateStandingOrderRequest;
import com.group1.banking.dto.response.CancelStandingOrderResponse;
import com.group1.banking.dto.response.StandingOrderListResponse;
import com.group1.banking.dto.response.StandingOrderResponse;
import com.group1.banking.exception.PermissionDeniedException;
import com.group1.banking.scheduler.StandingOrderExecutionJob;
import com.group1.banking.security.CustomUserPrincipal;
import com.group1.banking.service.StandingOrderService;
import jakarta.validation.Valid;
 
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
 
/**
* Standing order controller. (T053)
*/
@RestController
public class StandingOrderController {
 
    private final StandingOrderService standingOrderService;
    
    @Autowired
    private StandingOrderExecutionJob standingOrderExecutionJob;
 
    public StandingOrderController(StandingOrderService standingOrderService) {
        this.standingOrderService = standingOrderService;
    }
 
    @PostMapping("/accounts/{accountId}/standing-orders")
    public ResponseEntity<StandingOrderResponse> createStandingOrder(
            @PathVariable long accountId,
            @RequestBody @Valid CreateStandingOrderRequest request,
            @AuthenticationPrincipal CustomUserPrincipal principal) {
        StandingOrderResponse response = standingOrderService.create(accountId, request, extractPrincipal(principal));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
 
    @GetMapping("/accounts/{accountId}/standing-orders")
    public ResponseEntity<StandingOrderListResponse> listStandingOrders(
            @PathVariable long accountId,
            @AuthenticationPrincipal CustomUserPrincipal principal) {
        return ResponseEntity.ok(standingOrderService.list(accountId, extractPrincipal(principal)));
    }
 
    @DeleteMapping("/standing-orders/{standingOrderId}")
    public ResponseEntity<CancelStandingOrderResponse> cancelStandingOrder(
            @PathVariable String standingOrderId,
            @AuthenticationPrincipal CustomUserPrincipal principal) {
        return ResponseEntity.ok(standingOrderService.cancel(standingOrderId, extractPrincipal(principal)));
    }
    
    @GetMapping("/test-standing-order")
    public String testStandingOrder() {
        standingOrderExecutionJob.processOrders();
        return "Triggered";
    }
 
    private CustomUserPrincipal extractPrincipal(CustomUserPrincipal principal) {
        if (principal == null) {
            throw new PermissionDeniedException("AUTHENTICATION");
        }
        return principal;
    }
}
 