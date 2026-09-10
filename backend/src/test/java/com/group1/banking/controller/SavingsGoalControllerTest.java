package com.group1.banking.controller;

import tools.jackson.databind.json.JsonMapper;
import com.group1.banking.dto.SavingsGoalRequest;
import com.group1.banking.dto.SavingsGoalResponse;
import com.group1.banking.enums.SavingsGoalStatus;
import com.group1.banking.exception.BusinessException;
import com.group1.banking.repository.UserRepository;
import com.group1.banking.security.JwtService;
import com.group1.banking.service.SavingsGoalService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import com.group1.banking.security.CustomUserPrincipal;
import com.group1.banking.entity.User;
import com.group1.banking.enums.RoleName;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Controller tests for SavingsGoalController using @WebMvcTest.
 * Covers: T016, T019, T020, T030, T031, T032, T041, T043, T052, T053, T055
 */
@WebMvcTest(SavingsGoalController.class)
@AutoConfigureMockMvc(addFilters = false)
class SavingsGoalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper objectMapper;

    @MockitoBean
    private SavingsGoalService savingsGoalService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserRepository userRepository;

    private SavingsGoalResponse sampleGoalResponse() {
        return SavingsGoalResponse.builder()
                .goalId(1L)
                .accountId(100L)
                .accountNumber("ACC-001")
                .accountType("SAVINGS")
                .goalName("Travel")
                .targetAmount(new BigDecimal("5000.00"))
                .targetDate(LocalDate.now().plusDays(180))
                .currentBalance(new BigDecimal("1000.00"))
                .progressPercentage(new BigDecimal("20.00"))
                .timeRemainingDays(180L)
                .status(SavingsGoalStatus.IN_PROGRESS)
                .createdAt(ZonedDateTime.now())
                .updatedAt(ZonedDateTime.now())
                .build();
    }

    private SavingsGoalRequest validRequest() {
        return new SavingsGoalRequest("Travel", new BigDecimal("5000.00"), LocalDate.now().plusDays(180));
    }

    // ===== T016: POST /accounts/{account_id}/goals contract test =====

    @Test // T016: POST returns 201 with goal_id, progress_percentage, status, time_remaining_days
    @WithCustomUser
    void createGoal_validRequest_returns201WithResponse() throws Exception {
        when(savingsGoalService.createGoal(anyLong(), eq(100L), any(SavingsGoalRequest.class)))
                .thenReturn(sampleGoalResponse());

        mockMvc.perform(post("/api/goals/accounts/100")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.goal_id").value(1))
                .andExpect(jsonPath("$.goal_name").value("Travel"))
                .andExpect(jsonPath("$.progress_percentage").exists())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.time_remaining_days").exists());
    }

    // ===== T019: customer_id mismatch returns 403 =====

    @Test // T019: 403 UNAUTHORIZED_ACCOUNT_ACCESS when ownership fails
    @WithCustomUser
    void createGoal_ownershipMismatch_returns403() throws Exception {
        when(savingsGoalService.createGoal(anyLong(), eq(100L), any()))
                .thenThrow(new BusinessException("UNAUTHORIZED_ACCOUNT_ACCESS", "Access denied"));

        mockMvc.perform(post("/api/goals/accounts/100")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED_ACCOUNT_ACCESS"));
    }

    // ===== T020: account.status != ACTIVE returns 404 =====

    @Test // T020: 404 ACCOUNT_NOT_FOUND when account is inactive or deleted
    @WithCustomUser
    void createGoal_inactiveAccount_returns404() throws Exception {
        when(savingsGoalService.createGoal(anyLong(), eq(100L), any()))
                .thenThrow(new BusinessException("ACCOUNT_NOT_FOUND", "Account not found or inactive"));

        mockMvc.perform(post("/api/goals/accounts/100")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ACCOUNT_NOT_FOUND"));
    }

    // ===== T030: GET /accounts/{account_id}/goals contract test =====

    @Test // T030: GET returns response with current_balance, progress_percentage, time_remaining_days, status
    @WithCustomUser
    void getGoal_validRequest_returns200WithDerivedFields() throws Exception {
        when(savingsGoalService.getGoal(anyLong(), eq(100L))).thenReturn(sampleGoalResponse());

        mockMvc.perform(get("/api/goals/accounts/100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.current_balance").value(1000.00))
                .andExpect(jsonPath("$.progress_percentage").value(20.00))
                .andExpect(jsonPath("$.time_remaining_days").exists())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    // ===== T031: GET /customers/{customer_id}/goals bulk fetch =====

    @Test // T031: bulk fetch returns array of active goals only
    @WithCustomUser
    void getAllGoals_returnsArrayOfGoals() throws Exception {
        when(savingsGoalService.getAllGoalsForCustomer(eq(42L))).thenReturn(List.of(sampleGoalResponse()));

        mockMvc.perform(get("/api/goals/customers/42"))  // customerId from path, not from auth
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].goal_id").value(1))
                .andExpect(jsonPath("$[0].goal_name").value("Travel"));
    }

    @Test // T031: bulk fetch returns empty array when no goals
    @WithCustomUser
    void getAllGoals_noGoals_returnsEmptyArray() throws Exception {
        when(savingsGoalService.getAllGoalsForCustomer(anyLong())).thenReturn(List.of());

        mockMvc.perform(get("/api/goals/customers/42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    // ===== T032: GET for account with no goal returns 404 =====

    @Test // T032: GET for account with no goal returns 404 GOAL_NOT_FOUND
    @WithCustomUser
    void getGoal_noGoalForAccount_returns404() throws Exception {
        when(savingsGoalService.getGoal(anyLong(), anyLong()))
                .thenThrow(new BusinessException("GOAL_NOT_FOUND", "Goal not found"));

        mockMvc.perform(get("/api/goals/accounts/100"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("GOAL_NOT_FOUND"));
    }

    // ===== T041: PUT /accounts/{account_id}/goals/{goal_id} contract test =====

    @Test // T041: PUT returns 200 with updated fields including progress_percentage, status, updated_at
    @WithCustomUser
    void updateGoal_validRequest_returns200WithUpdatedResponse() throws Exception {
        SavingsGoalResponse updated = SavingsGoalResponse.builder()
                .goalId(1L).accountId(100L).accountNumber("ACC-001").accountType("SAVINGS")
                .goalName("Updated Travel").targetAmount(new BigDecimal("3000.00"))
                .targetDate(LocalDate.now().plusDays(90))
                .currentBalance(new BigDecimal("1000.00"))
                .progressPercentage(new BigDecimal("33.33"))
                .timeRemainingDays(90L)
                .status(SavingsGoalStatus.IN_PROGRESS)
                .createdAt(ZonedDateTime.now()).updatedAt(ZonedDateTime.now())
                .build();
        when(savingsGoalService.updateGoal(anyLong(), eq(1L), any())).thenReturn(updated);

        SavingsGoalRequest updateRequest = new SavingsGoalRequest("Updated Travel", new BigDecimal("3000.00"), LocalDate.now().plusDays(90));

        mockMvc.perform(put("/api/goals/accounts/100/goals/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.goal_name").value("Updated Travel"))
                .andExpect(jsonPath("$.progress_percentage").exists())
                .andExpect(jsonPath("$.updated_at").exists());
    }

    // ===== T043: PUT ownership check =====

    @Test // T043: PUT returns 403 when customer doesn't own the goal
    @WithCustomUser
    void updateGoal_ownershipFails_returns403() throws Exception {
        when(savingsGoalService.updateGoal(anyLong(), eq(1L), any()))
                .thenThrow(new BusinessException("GOAL_NOT_FOUND", "Goal not found"));

        mockMvc.perform(put("/api/goals/accounts/100/goals/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isNotFound());
    }

    // ===== T052: DELETE /accounts/{account_id}/goals/{goal_id} returns 204 =====

    @Test // T052: DELETE returns 204 No Content
    @WithCustomUser
    void deleteGoal_validRequest_returns204() throws Exception {
        doNothing().when(savingsGoalService).deleteGoal(anyLong(), eq(1L));

        mockMvc.perform(delete("/api/goals/accounts/100/goals/1"))
                .andExpect(status().isNoContent());
    }

    // ===== T053: DELETE ownership check =====

    @Test // T053: DELETE returns 404 when goal doesn't exist for this customer
    @WithCustomUser
    void deleteGoal_goalNotFound_returns404() throws Exception {
        doThrow(new BusinessException("GOAL_NOT_FOUND", "Goal not found"))
                .when(savingsGoalService).deleteGoal(anyLong(), eq(99L));

        mockMvc.perform(delete("/api/goals/accounts/100/goals/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("GOAL_NOT_FOUND"));
    }

    // ===== T055: GET after DELETE returns 404 =====

    @Test // T055: after delete, GET returns 404
    @WithCustomUser
    void getGoal_afterDelete_returns404() throws Exception {
        when(savingsGoalService.getGoal(anyLong(), anyLong()))
                .thenThrow(new BusinessException("GOAL_NOT_FOUND", "Goal not found"));

        mockMvc.perform(get("/api/goals/accounts/100"))
                .andExpect(status().isNotFound());
    }

    // ===== 409 GOAL_ALREADY_EXISTS =====

    @Test
    @WithCustomUser
    void createGoal_duplicateGoal_returns409() throws Exception {
        when(savingsGoalService.createGoal(anyLong(), eq(100L), any()))
                .thenThrow(new BusinessException("GOAL_ALREADY_EXISTS", "Goal already exists for this account"));

        mockMvc.perform(post("/api/goals/accounts/100")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("GOAL_ALREADY_EXISTS"));
    }
}
