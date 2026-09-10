package com.group1.banking.controller;

import com.group1.banking.dto.response.TransactionHistoryResponse;
import com.group1.banking.repository.UserRepository;
import com.group1.banking.security.JwtService;
import com.group1.banking.service.TransactionHistoryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TransactionController.class)
@AutoConfigureMockMvc(addFilters = false)
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TransactionHistoryService transactionHistoryService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserRepository userRepository;

    @Test
    @WithCustomUser
    void getTransactionHistory_shouldReturn200() throws Exception {
        TransactionHistoryResponse response = new TransactionHistoryResponse();
        response.setAccountId(1001L);
        response.setTransactionCount(0);
        response.setTransactions(List.of());

        when(transactionHistoryService.getHistory(anyLong(), any(), any(), any()))
                .thenReturn(response);

        mockMvc.perform(get("/accounts/1001/transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountId").value(1001))
                .andExpect(jsonPath("$.transactionCount").value(0));
    }

    @Test
    @WithCustomUser
    void getTransactionHistory_shouldReturn200_withDateParams() throws Exception {
        TransactionHistoryResponse response = new TransactionHistoryResponse();
        response.setAccountId(1001L);
        response.setTransactionCount(0);
        response.setTransactions(List.of());

        when(transactionHistoryService.getHistory(anyLong(), any(), any(), any()))
                .thenReturn(response);

        mockMvc.perform(get("/accounts/1001/transactions")
                        .param("startDate", "2025-01-01")
                        .param("endDate", "2025-01-31"))
                .andExpect(status().isOk());
    }

    @Test
    @WithCustomUser
    void exportTransactionsAsPdf_shouldReturn200_withPdfContentType() throws Exception {
        byte[] fakeBytes = new byte[]{1, 2, 3};
        when(transactionHistoryService.exportPdf(anyLong(), any(), any(), any()))
                .thenReturn(fakeBytes);

        mockMvc.perform(get("/accounts/1001/transactions/export"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string("Content-Disposition", containsString("statement-1001")));
    }

    @Test
    @WithCustomUser
    void exportTransactionsAsPdf_shouldIncludeDateRangeInFilename() throws Exception {
        byte[] fakeBytes = new byte[]{1, 2, 3};
        when(transactionHistoryService.exportPdf(anyLong(), any(), any(), any()))
                .thenReturn(fakeBytes);

        mockMvc.perform(get("/accounts/1001/transactions/export")
                        .param("startDate", "2025-01-01")
                        .param("endDate", "2025-01-31"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("2025-01-01")));
    }
}
