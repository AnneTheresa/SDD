package com.group1.banking.controller;

import tools.jackson.databind.json.JsonMapper;
import com.group1.banking.dto.request.CreateStandingOrderRequest;
import com.group1.banking.dto.response.CancelStandingOrderResponse;
import com.group1.banking.dto.response.StandingOrderListResponse;
import com.group1.banking.dto.response.StandingOrderResponse;
import com.group1.banking.exception.ResourceNotFoundException;
import com.group1.banking.repository.UserRepository;
import com.group1.banking.scheduler.StandingOrderExecutionJob;
import com.group1.banking.security.CustomUserPrincipal;
import com.group1.banking.security.JwtService;
import com.group1.banking.service.StandingOrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Controller tests for StandingOrderController using @WebMvcTest.
 */
@WebMvcTest(StandingOrderController.class)
@AutoConfigureMockMvc(addFilters = false)
class StandingOrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper objectMapper;

    @MockitoBean
    private StandingOrderService standingOrderService;

    @MockitoBean
    private StandingOrderExecutionJob standingOrderExecutionJob;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserRepository userRepository;

    private StandingOrderResponse sampleOrder() {
        StandingOrderResponse r = new StandingOrderResponse();
        r.setStandingOrderId("so-001");
        r.setSourceAccountId(1001L);
        r.setPayeeAccount(9999L);
        r.setPayeeName("Landlord");
        r.setAmount(new BigDecimal("500.00"));
        r.setFrequency("MONTHLY");
        r.setStatus("ACTIVE");
        r.setNextRunDate(LocalDateTime.now().plusDays(30));
        r.setReference("RENT001");
        r.setMessage("Standing order created");
        return r;
    }

    // ===== CREATE STANDING ORDER =====

    @Test
    @WithCustomUser
    void createStandingOrder_shouldReturn201_whenValidRequest() throws Exception {
        CreateStandingOrderRequest request = new CreateStandingOrderRequest();
        request.setPayeeAccount(9999L);
        request.setPayeeName("Landlord");
        request.setAmount(new BigDecimal("500.00"));
        request.setFrequency("MONTHLY");
        request.setStartDate(LocalDateTime.now().plusDays(2));
        request.setReference("RENT001");

        when(standingOrderService.create(eq(1001L), any(CreateStandingOrderRequest.class), any(CustomUserPrincipal.class)))
                .thenReturn(sampleOrder());

        mockMvc.perform(post("/accounts/1001/standing-orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.standingOrderId").value("so-001"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @WithCustomUser
    void createStandingOrder_shouldReturn422_whenPayeeNameMissing() throws Exception {
        CreateStandingOrderRequest request = new CreateStandingOrderRequest();
        request.setPayeeAccount(9999L);
        request.setAmount(new BigDecimal("500.00"));
        request.setFrequency("MONTHLY");
        request.setStartDate(LocalDateTime.now().plusDays(2));
        request.setReference("RENT001");

        mockMvc.perform(post("/accounts/1001/standing-orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableContent());
    }

    @Test
    @WithCustomUser
    void createStandingOrder_shouldReturn422_whenAmountMissing() throws Exception {
        CreateStandingOrderRequest request = new CreateStandingOrderRequest();
        request.setPayeeAccount(9999L);
        request.setPayeeName("Landlord");
        request.setFrequency("MONTHLY");
        request.setStartDate(LocalDateTime.now().plusDays(2));
        request.setReference("RENT001");

        mockMvc.perform(post("/accounts/1001/standing-orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableContent());
    }

    @Test
    @WithCustomUser
    void createStandingOrder_shouldReturn422_whenReferenceMissing() throws Exception {
        CreateStandingOrderRequest request = new CreateStandingOrderRequest();
        request.setPayeeAccount(9999L);
        request.setPayeeName("Landlord");
        request.setAmount(new BigDecimal("500.00"));
        request.setFrequency("MONTHLY");
        request.setStartDate(LocalDateTime.now().plusDays(2));

        mockMvc.perform(post("/accounts/1001/standing-orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableContent());
    }

    // ===== LIST STANDING ORDERS =====

    @Test
    @WithCustomUser
    void listStandingOrders_shouldReturn200_withOrders() throws Exception {
        StandingOrderListResponse listResponse = new StandingOrderListResponse();
        listResponse.setAccountId(1001L);
        listResponse.setStandingOrderCount(1);
        listResponse.setStandingOrders(List.of(sampleOrder()));

        when(standingOrderService.list(eq(1001L), any(CustomUserPrincipal.class))).thenReturn(listResponse);

        mockMvc.perform(get("/accounts/1001/standing-orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountId").value(1001))
                .andExpect(jsonPath("$.standingOrderCount").value(1))
                .andExpect(jsonPath("$.standingOrders").isArray());
    }

    // ===== CANCEL STANDING ORDER =====

    @Test
    @WithCustomUser
    void cancelStandingOrder_shouldReturn200_whenCancelled() throws Exception {
        CancelStandingOrderResponse cancelResponse = new CancelStandingOrderResponse();
        cancelResponse.setStandingOrderId("so-001");
        cancelResponse.setStatus("CANCELLED");
        cancelResponse.setMessage("Standing order cancelled successfully");

        when(standingOrderService.cancel(eq("so-001"), any(CustomUserPrincipal.class))).thenReturn(cancelResponse);

        mockMvc.perform(delete("/standing-orders/so-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.standingOrderId").value("so-001"))
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    @WithCustomUser
    void cancelStandingOrder_shouldReturn404_whenNotFound() throws Exception {
        when(standingOrderService.cancel(eq("so-999"), any(CustomUserPrincipal.class)))
                .thenThrow(new ResourceNotFoundException("Not found", "ERR_NOT_FOUND", null));

        mockMvc.perform(delete("/standing-orders/so-999"))
                .andExpect(status().isNotFound());
    }
}
