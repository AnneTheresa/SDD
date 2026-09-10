package com.group1.banking.controller;

import tools.jackson.databind.json.JsonMapper;
import com.group1.banking.dto.gic.CreateGicRequest;
import com.group1.banking.dto.gic.GicResponse;
import com.group1.banking.dto.gic.RedeemGicResponse;
import com.group1.banking.entity.GicStatus;
import com.group1.banking.entity.GicTerm;
import com.group1.banking.exception.BadRequestException;
import com.group1.banking.exception.NotFoundException;
import com.group1.banking.repository.UserRepository;
import com.group1.banking.security.JwtService;
import com.group1.banking.service.impl.GicService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Controller tests for GicController using @WebMvcTest.
 *
 * POST   /accounts/{accountId}/gic                — create GIC
 * GET    /accounts/{accountId}/gic                — list GICs
 * POST   /accounts/{accountId}/gic/{gicId}/redeem — redeem a GIC
 */
@WebMvcTest(GicController.class)
@AutoConfigureMockMvc(addFilters = false)
class GicControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper objectMapper;

    @MockitoBean
    private GicService gicService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserRepository userRepository;

    private GicResponse sampleGicResponse() {
        return new GicResponse(
                "GIC-TESTID001",
                1001L,
                new BigDecimal("1000.00"),
                new BigDecimal("0.0500"),
                GicTerm.ONE_YEAR,
                LocalDate.now(),
                LocalDate.now().plusYears(1),
                new BigDecimal("1050.00"),
                GicStatus.ACTIVE,
                null,
                Instant.now(),
                Instant.now());
    }

    // ===== CREATE GIC =====

    @Test
    void createGic_shouldReturn201_whenValidRequest() throws Exception {
        CreateGicRequest request = new CreateGicRequest(new BigDecimal("1000.00"), GicTerm.ONE_YEAR);

        when(gicService.createGic(eq(1001L), any(CreateGicRequest.class)))
                .thenReturn(sampleGicResponse());

        mockMvc.perform(post("/accounts/1001/gic")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.gicId").value("GIC-TESTID001"))
                .andExpect(jsonPath("$.accountId").value(1001))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void createGic_shouldReturn422_whenAmountMissing() throws Exception {
        String body = "{\"term\":\"ONE_YEAR\"}";

        mockMvc.perform(post("/accounts/1001/gic")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnprocessableContent());
    }

    @Test
    void createGic_shouldReturn422_whenTermMissing() throws Exception {
        String body = "{\"amount\":1000.00}";

        mockMvc.perform(post("/accounts/1001/gic")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnprocessableContent());
    }

    @Test
    void createGic_shouldReturn422_whenAmountIsZero() throws Exception {
        CreateGicRequest request = new CreateGicRequest(BigDecimal.ZERO, GicTerm.ONE_YEAR);

        mockMvc.perform(post("/accounts/1001/gic")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableContent());
    }

    @Test
    void createGic_shouldReturn404_whenAccountNotFound() throws Exception {
        CreateGicRequest request = new CreateGicRequest(new BigDecimal("1000.00"), GicTerm.ONE_YEAR);

        when(gicService.createGic(eq(999L), any(CreateGicRequest.class)))
                .thenThrow(new NotFoundException("ACCOUNT_NOT_FOUND", "Account not found.", Map.of("accountId", 999L)));

        mockMvc.perform(post("/accounts/999/gic")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void createGic_shouldReturn400_whenInsufficientFunds() throws Exception {
        CreateGicRequest request = new CreateGicRequest(new BigDecimal("99999.00"), GicTerm.ONE_YEAR);

        when(gicService.createGic(eq(1001L), any(CreateGicRequest.class)))
                .thenThrow(new BadRequestException("INSUFFICIENT_FUNDS", "Insufficient RRSP balance.", null));

        mockMvc.perform(post("/accounts/1001/gic")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ===== GET GICs =====

    @Test
    void getGics_shouldReturn200_withGicList() throws Exception {
        when(gicService.getGics(1001L)).thenReturn(List.of(sampleGicResponse()));

        mockMvc.perform(get("/accounts/1001/gic"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].gicId").value("GIC-TESTID001"))
                .andExpect(jsonPath("$[0].status").value("ACTIVE"));
    }

    @Test
    void getGics_shouldReturn200_withEmptyList() throws Exception {
        when(gicService.getGics(1001L)).thenReturn(List.of());

        mockMvc.perform(get("/accounts/1001/gic"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void getGics_shouldReturn200_withMultipleGics() throws Exception {
        GicResponse gic2 = new GicResponse(
                "GIC-TESTID002",
                1001L,
                new BigDecimal("2000.00"),
                new BigDecimal("0.0550"),
                GicTerm.TWO_YEARS,
                LocalDate.now(),
                LocalDate.now().plusYears(2),
                new BigDecimal("2220.00"),
                GicStatus.ACTIVE,
                null,
                Instant.now(),
                Instant.now());

        when(gicService.getGics(1001L)).thenReturn(List.of(sampleGicResponse(), gic2));

        mockMvc.perform(get("/accounts/1001/gic"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void getGics_shouldReturn404_whenAccountNotFound() throws Exception {
        when(gicService.getGics(999L))
                .thenThrow(new NotFoundException("ACCOUNT_NOT_FOUND", "Account not found.", Map.of("accountId", 999L)));

        mockMvc.perform(get("/accounts/999/gic"))
                .andExpect(status().isNotFound());
    }

    // ===== REDEEM GIC =====

    @Test
    void redeemGic_shouldReturn200_whenSuccessful() throws Exception {
        RedeemGicResponse response = new RedeemGicResponse("GIC redeemed successfully.", new BigDecimal("1050.00"));

        when(gicService.redeemGic(1001L, "GIC-TESTID001")).thenReturn(response);

        mockMvc.perform(post("/accounts/1001/gic/GIC-TESTID001/redeem"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("GIC redeemed successfully."))
                .andExpect(jsonPath("$.payoutAmount").value(1050.00));
    }

    @Test
    void redeemGic_shouldReturn404_whenGicNotFound() throws Exception {
        when(gicService.redeemGic(1001L, "NONEXISTENT"))
                .thenThrow(new NotFoundException("GIC_NOT_FOUND", "GIC not found.", Map.of("gicId", "NONEXISTENT")));

        mockMvc.perform(post("/accounts/1001/gic/NONEXISTENT/redeem"))
                .andExpect(status().isNotFound());
    }

    @Test
    void redeemGic_shouldReturn400_whenAlreadyRedeemed() throws Exception {
        when(gicService.redeemGic(1001L, "GIC-TESTID001"))
                .thenThrow(new BadRequestException("GIC_ALREADY_REDEEMED", "This GIC has already been redeemed.", null));

        mockMvc.perform(post("/accounts/1001/gic/GIC-TESTID001/redeem"))
                .andExpect(status().isBadRequest());
    }
}
