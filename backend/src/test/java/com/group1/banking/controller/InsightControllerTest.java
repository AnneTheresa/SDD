package com.group1.banking.controller;

import tools.jackson.databind.json.JsonMapper;
import com.group1.banking.dto.request.RecategoriseRequest;
import com.group1.banking.dto.response.RecategoriseResponse;
import com.group1.banking.dto.response.SpendingInsightResponse;
import com.group1.banking.repository.UserRepository;
import com.group1.banking.security.CustomUserPrincipal;
import com.group1.banking.security.JwtService;
import com.group1.banking.service.SpendingInsightService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(InsightController.class)
@AutoConfigureMockMvc(addFilters = false)
class InsightControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper objectMapper;

    @MockitoBean
    private SpendingInsightService spendingInsightService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserRepository userRepository;

    @Test
    @WithCustomUser
    void getInsights_shouldReturn200_whenValidRequest() throws Exception {
        SpendingInsightResponse response = new SpendingInsightResponse();
        response.setAccountId(1001L);
        response.setTotalDebitSpend(new BigDecimal("250.00"));
        response.setCategoryBreakdown(List.of());
        response.setTopTransactions(List.of());
        response.setSixMonthTrend(List.of());

        when(spendingInsightService.getInsights(eq(1001L), eq(2025), eq(3), any(CustomUserPrincipal.class)))
                .thenReturn(response);

        mockMvc.perform(get("/accounts/1001/insights")
                        .param("year", "2025")
                        .param("month", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountId").value(1001));
    }

    @Test
    @WithCustomUser
    void getInsights_shouldReturn200_forDifferentYearMonth() throws Exception {
        SpendingInsightResponse response = new SpendingInsightResponse();
        response.setAccountId(1001L);
        response.setCategoryBreakdown(List.of());
        response.setTopTransactions(List.of());
        response.setSixMonthTrend(List.of());

        when(spendingInsightService.getInsights(anyLong(), anyInt(), anyInt(), any(CustomUserPrincipal.class)))
                .thenReturn(response);

        mockMvc.perform(get("/accounts/1001/insights")
                        .param("year", "2024")
                        .param("month", "12"))
                .andExpect(status().isOk());
    }

    @Test
    @WithCustomUser
    void recategorise_shouldReturn200_whenValidRequest() throws Exception {
        RecategoriseRequest request = new RecategoriseRequest();
        request.setCategory("Groceries");

        RecategoriseResponse response = new RecategoriseResponse();
        response.setTransactionId("tx-001");
        response.setPreviousCategory("Uncategorised");
        response.setUpdatedCategory("Groceries");

        when(spendingInsightService.recategorise(eq(1001L), eq("tx-001"), eq("Groceries"), any(CustomUserPrincipal.class)))
                .thenReturn(response);

        mockMvc.perform(put("/accounts/1001/transactions/tx-001/category")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionId").value("tx-001"))
                .andExpect(jsonPath("$.updatedCategory").value("Groceries"));
    }

    @Test
    @WithCustomUser
    void recategorise_shouldReturn422_whenCategoryIsBlank() throws Exception {
        RecategoriseRequest request = new RecategoriseRequest();
        request.setCategory("");

        mockMvc.perform(put("/accounts/1001/transactions/tx-001/category")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableContent());
    }

    @Test
    @WithCustomUser
    void recategorise_shouldReturn422_whenCategoryIsMissing() throws Exception {
        mockMvc.perform(put("/accounts/1001/transactions/tx-001/category")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnprocessableContent());
    }
}
