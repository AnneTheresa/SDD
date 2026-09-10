package com.group1.banking.controller;

import com.group1.banking.repository.UserRepository;
import com.group1.banking.security.CustomUserPrincipal;
import com.group1.banking.security.JwtService;
import com.group1.banking.service.MonthlyStatementService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(StatementController.class)
@AutoConfigureMockMvc(addFilters = false)
class StatementControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MonthlyStatementService monthlyStatementService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserRepository userRepository;

    @Test
    @WithCustomUser
    void getStatement_shouldReturn200_withPdfContent() throws Exception {
        byte[] fakePdf = new byte[]{1, 2, 3, 4};
        when(monthlyStatementService.generateStatement(eq(1001L), eq("2025-03"), any(CustomUserPrincipal.class)))
                .thenReturn(fakePdf);

        mockMvc.perform(get("/accounts/1001/statements/2025-03"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string("Content-Disposition", containsString("statement-1001-2025-03.pdf")));
    }

    @Test
    @WithCustomUser
    void getStatement_shouldReturn200_forDifferentPeriod() throws Exception {
        byte[] fakePdf = new byte[]{5, 6, 7};
        when(monthlyStatementService.generateStatement(anyLong(), eq("2024-12"), any(CustomUserPrincipal.class)))
                .thenReturn(fakePdf);

        mockMvc.perform(get("/accounts/2002/statements/2024-12"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF));
    }

    @Test
    @WithCustomUser
    void getStatement_shouldDelegateToServiceWithCorrectArgs() throws Exception {
        byte[] fakePdf = new byte[]{1};
        when(monthlyStatementService.generateStatement(anyLong(), anyString(), any(CustomUserPrincipal.class)))
                .thenReturn(fakePdf);

        mockMvc.perform(get("/accounts/1001/statements/2025-01"))
                .andExpect(status().isOk());

        verify(monthlyStatementService).generateStatement(eq(1001L), eq("2025-01"), any(CustomUserPrincipal.class));
    }
}
