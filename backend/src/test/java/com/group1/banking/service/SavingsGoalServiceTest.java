package com.group1.banking.service;

import com.group1.banking.dto.SavingsGoalRequest;
import com.group1.banking.dto.SavingsGoalResponse;
import com.group1.banking.entity.*;
import com.group1.banking.enums.SavingsGoalStatus;
import com.group1.banking.exception.BusinessException;
import com.group1.banking.repository.AccountRepository;
import com.group1.banking.repository.SavingsGoalRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for SavingsGoalService
 * Covers: T017, T018, T033, T042, T044, T054, T063, T064, T065, T066
 */
@ExtendWith(MockitoExtension.class)
class SavingsGoalServiceTest {

    @Mock
    private SavingsGoalRepository savingsGoalRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private SavingsGoalService savingsGoalService;

    private Customer customer;
    private Account account;
    private SavingsGoal existingGoal;

    @BeforeEach
    void setUp() {
        customer = new Customer();
        customer.setCustomerId(1L);

        account = new Account();
        account.setAccountId(100L);
        account.setAccountNumber("ACC-001");
        account.setAccountType(AccountType.SAVINGS);
        account.setStatus(AccountStatus.ACTIVE);
        account.setBalance(new BigDecimal("1000.00"));
        account.setCustomer(customer);

        existingGoal = new SavingsGoal();
        existingGoal.setGoalId(1L);
        existingGoal.setCustomerId(1L);
        existingGoal.setAccount(account);
        existingGoal.setGoalName("Travel");
        existingGoal.setTargetAmount(new BigDecimal("5000.00"));
        existingGoal.setTargetDate(LocalDate.now().plusMonths(6));
        existingGoal.setStatus(SavingsGoalStatus.IN_PROGRESS);
        existingGoal.setDeletedAt(null);
        existingGoal.setCreatedAt(Instant.now());
        existingGoal.setUpdatedAt(Instant.now());
    }

    // ===== CREATE GOAL VALIDATION (T017, T018) =====

    @Test // T017: target_amount <= 0 returns 400 INVALID_TARGET_AMOUNT
    void createGoal_targetAmountZero_throwsInvalidTargetAmount() {
        when(accountRepository.findById(100L)).thenReturn(Optional.of(account));
        when(savingsGoalRepository.findActiveByCustomerIdAndAccountId(1L, 100L))
                .thenReturn(Optional.empty());

        SavingsGoalRequest request = new SavingsGoalRequest("Travel", BigDecimal.ZERO, LocalDate.now().plusDays(30));

        assertThatThrownBy(() -> savingsGoalService.createGoal(1L, 100L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Target amount")
                .extracting(e -> ((BusinessException) e).getCode())
                .isEqualTo("INVALID_TARGET_AMOUNT");
    }

    @Test // T017: negative target_amount
    void createGoal_negativeTargetAmount_throwsInvalidTargetAmount() {
        when(accountRepository.findById(100L)).thenReturn(Optional.of(account));
        when(savingsGoalRepository.findActiveByCustomerIdAndAccountId(1L, 100L))
                .thenReturn(Optional.empty());

        SavingsGoalRequest request = new SavingsGoalRequest("Travel", new BigDecimal("-100.00"), LocalDate.now().plusDays(30));

        assertThatThrownBy(() -> savingsGoalService.createGoal(1L, 100L, request))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getCode())
                .isEqualTo("INVALID_TARGET_AMOUNT");
    }

    @Test // target_amount with more than 2 decimal places is rejected before persistence
    void createGoal_moreThanTwoDecimalPlaces_throwsInvalidTargetAmount() {
        when(accountRepository.findById(100L)).thenReturn(Optional.of(account));
        when(savingsGoalRepository.findActiveByCustomerIdAndAccountId(1L, 100L))
                .thenReturn(Optional.empty());

        SavingsGoalRequest request = new SavingsGoalRequest("Travel", new BigDecimal("10.001"), LocalDate.now().plusDays(30));

        assertThatThrownBy(() -> savingsGoalService.createGoal(1L, 100L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("2 decimal places")
                .extracting(e -> ((BusinessException) e).getCode())
                .isEqualTo("INVALID_TARGET_AMOUNT");
    }

    @Test // T018: target_date in past returns 400 INVALID_TARGET_DATE (CREATE only)
    void createGoal_pastTargetDate_throwsInvalidTargetDate() {
        when(accountRepository.findById(100L)).thenReturn(Optional.of(account));
        when(savingsGoalRepository.findActiveByCustomerIdAndAccountId(1L, 100L))
                .thenReturn(Optional.empty());

        SavingsGoalRequest request = new SavingsGoalRequest("Travel", new BigDecimal("5000.00"), LocalDate.now().minusDays(1));

        assertThatThrownBy(() -> savingsGoalService.createGoal(1L, 100L, request))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getCode())
                .isEqualTo("INVALID_TARGET_DATE");
    }

    @Test // goal_name empty returns 400 INVALID_GOAL_NAME
    void createGoal_emptyGoalName_throwsInvalidGoalName() {
        when(accountRepository.findById(100L)).thenReturn(Optional.of(account));
        when(savingsGoalRepository.findActiveByCustomerIdAndAccountId(1L, 100L))
                .thenReturn(Optional.empty());

        SavingsGoalRequest request = new SavingsGoalRequest("  ", new BigDecimal("5000.00"), LocalDate.now().plusDays(30));

        assertThatThrownBy(() -> savingsGoalService.createGoal(1L, 100L, request))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getCode())
                .isEqualTo("INVALID_GOAL_NAME");
    }

    @Test // account not found returns 404 ACCOUNT_NOT_FOUND
    void createGoal_accountNotFound_throwsAccountNotFound() {
        when(accountRepository.findById(999L)).thenReturn(Optional.empty());

        SavingsGoalRequest request = new SavingsGoalRequest("Travel", new BigDecimal("5000.00"), LocalDate.now().plusDays(30));

        assertThatThrownBy(() -> savingsGoalService.createGoal(1L, 999L, request))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getCode())
                .isEqualTo("ACCOUNT_NOT_FOUND");
    }

    @Test // duplicate goal returns 409 GOAL_ALREADY_EXISTS
    void createGoal_duplicateGoal_throwsGoalAlreadyExists() {
        when(accountRepository.findById(100L)).thenReturn(Optional.of(account));
        when(savingsGoalRepository.findActiveByCustomerIdAndAccountId(1L, 100L))
                .thenReturn(Optional.of(existingGoal));

        SavingsGoalRequest request = new SavingsGoalRequest("Travel", new BigDecimal("5000.00"), LocalDate.now().plusDays(30));

        assertThatThrownBy(() -> savingsGoalService.createGoal(1L, 100L, request))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getCode())
                .isEqualTo("GOAL_ALREADY_EXISTS");
    }

    @Test // ownership mismatch returns 403
    void createGoal_ownershipMismatch_throwsUnauthorizedAccess() {
        when(accountRepository.findById(100L)).thenReturn(Optional.of(account));
        // Account belongs to customer 1, but we're calling with customer 2

        SavingsGoalRequest request = new SavingsGoalRequest("Travel", new BigDecimal("5000.00"), LocalDate.now().plusDays(30));

        assertThatThrownBy(() -> savingsGoalService.createGoal(2L, 100L, request))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getCode())
                .isEqualTo("UNAUTHORIZED_ACCOUNT_ACCESS");
    }

    @Test // happy path create goal
    void createGoal_validRequest_returnsGoalResponse() {
        when(accountRepository.findById(100L)).thenReturn(Optional.of(account));
        when(savingsGoalRepository.findActiveByCustomerIdAndAccountId(1L, 100L))
                .thenReturn(Optional.empty());
        when(savingsGoalRepository.save(any(SavingsGoal.class))).thenAnswer(inv -> {
            SavingsGoal g = inv.getArgument(0);
            g.setGoalId(1L);
            g.setCreatedAt(Instant.now());
            g.setUpdatedAt(Instant.now());
            return g;
        });
        doNothing().when(auditService).log(any(), any(), any(), any(), any(), any(), any(), any());

        SavingsGoalRequest request = new SavingsGoalRequest("Travel", new BigDecimal("5000.00"), LocalDate.now().plusDays(180));
        SavingsGoalResponse response = savingsGoalService.createGoal(1L, 100L, request);

        assertThat(response).isNotNull();
        assertThat(response.getGoalName()).isEqualTo("Travel");
        assertThat(response.getTargetAmount()).isEqualByComparingTo("5000.00");
        assertThat(response.getProgressPercentage()).isEqualByComparingTo("20.00"); // 1000/5000 * 100 = 20%
        assertThat(response.getStatus()).isEqualTo(SavingsGoalStatus.IN_PROGRESS);
    }

    // ===== PROGRESS CALCULATION (T063, T064) =====

    @Test // T063: (1000 / 5000) * 100 = 20%
    void progressCalculation_partialBalance_returns20Percent() {
        account.setBalance(new BigDecimal("1000.00"));
        when(accountRepository.findById(100L)).thenReturn(Optional.of(account));
        when(savingsGoalRepository.findActiveByCustomerIdAndAccountId(1L, 100L)).thenReturn(Optional.empty());
        when(savingsGoalRepository.save(any())).thenAnswer(inv -> {
            SavingsGoal g = inv.getArgument(0);
            g.setGoalId(1L); g.setCreatedAt(Instant.now()); g.setUpdatedAt(Instant.now());
            return g;
        });
        doNothing().when(auditService).log(any(), any(), any(), any(), any(), any(), any(), any());

        SavingsGoalRequest request = new SavingsGoalRequest("Travel", new BigDecimal("5000.00"), LocalDate.now().plusDays(90));
        SavingsGoalResponse response = savingsGoalService.createGoal(1L, 100L, request);

        assertThat(response.getProgressPercentage()).isGreaterThanOrEqualTo(new BigDecimal("19"))
                .isLessThanOrEqualTo(new BigDecimal("21"));
    }

    @Test // T064: progress never exceeds 100% (6000 balance / 5000 target = 100%, not 120%)
    void progressCalculation_balanceExceedsTarget_cappedAt100() {
        account.setBalance(new BigDecimal("6000.00"));
        when(accountRepository.findById(100L)).thenReturn(Optional.of(account));
        when(savingsGoalRepository.findActiveByCustomerIdAndAccountId(1L, 100L)).thenReturn(Optional.empty());
        when(savingsGoalRepository.save(any())).thenAnswer(inv -> {
            SavingsGoal g = inv.getArgument(0);
            g.setGoalId(1L); g.setCreatedAt(Instant.now()); g.setUpdatedAt(Instant.now());
            return g;
        });
        doNothing().when(auditService).log(any(), any(), any(), any(), any(), any(), any(), any());

        SavingsGoalRequest request = new SavingsGoalRequest("Travel", new BigDecimal("5000.00"), LocalDate.now().plusDays(90));
        SavingsGoalResponse response = savingsGoalService.createGoal(1L, 100L, request);

        assertThat(response.getProgressPercentage()).isEqualByComparingTo("100");
    }

    // ===== STATUS DERIVATION (T065, T066) =====

    @Test // T065: balance >= target_amount => status = ACHIEVED
    void statusDerivation_balanceEqualsTarget_statusIsAchieved() {
        account.setBalance(new BigDecimal("5000.00"));
        when(accountRepository.findById(100L)).thenReturn(Optional.of(account));
        when(savingsGoalRepository.findActiveByCustomerIdAndAccountId(1L, 100L)).thenReturn(Optional.empty());
        when(savingsGoalRepository.save(any())).thenAnswer(inv -> {
            SavingsGoal g = inv.getArgument(0);
            g.setGoalId(1L); g.setCreatedAt(Instant.now()); g.setUpdatedAt(Instant.now());
            return g;
        });
        doNothing().when(auditService).log(any(), any(), any(), any(), any(), any(), any(), any());

        SavingsGoalRequest request = new SavingsGoalRequest("Travel", new BigDecimal("5000.00"), LocalDate.now().plusDays(90));
        SavingsGoalResponse response = savingsGoalService.createGoal(1L, 100L, request);

        assertThat(response.getStatus()).isEqualTo(SavingsGoalStatus.ACHIEVED);
    }

    @Test // T066: target_date < today AND balance < target => OVERDUE, time_remaining_days = 0
    void statusDerivation_overdueGoal_statusIsOverdueAndTimeRemainingIsZero() {
        account.setBalance(new BigDecimal("1000.00"));
        existingGoal.setTargetDate(LocalDate.now().minusDays(1)); // past date
        when(savingsGoalRepository.findByGoalIdAndCustomerId(1L, 1L)).thenReturn(Optional.of(existingGoal));

        SavingsGoalResponse response = savingsGoalService.getGoal(1L, 1L);

        assertThat(response.getStatus()).isEqualTo(SavingsGoalStatus.OVERDUE);
        assertThat(response.getTimeRemainingDays()).isEqualTo(0L);
    }

    @Test // balance = 0 => NOT_STARTED
    void statusDerivation_zeroBalance_statusIsNotStarted() {
        account.setBalance(BigDecimal.ZERO);
        when(savingsGoalRepository.findByGoalIdAndCustomerId(1L, 1L)).thenReturn(Optional.of(existingGoal));

        SavingsGoalResponse response = savingsGoalService.getGoal(1L, 1L);

        assertThat(response.getStatus()).isEqualTo(SavingsGoalStatus.NOT_STARTED);
    }

    // ===== T033: Live balance (read from account, not cached) =====

    @Test // T033: progress reflects current account.balance, not a stale value
    void getGoal_usesLiveAccountBalance() {
        account.setBalance(new BigDecimal("2500.00")); // changed after creation
        when(savingsGoalRepository.findByGoalIdAndCustomerId(1L, 1L)).thenReturn(Optional.of(existingGoal));

        SavingsGoalResponse response = savingsGoalService.getGoal(1L, 1L);

        // 2500 / 5000 * 100 = 50%
        assertThat(response.getCurrentBalance()).isEqualByComparingTo("2500.00");
        assertThat(response.getProgressPercentage()).isEqualByComparingTo("50.00");
    }

    // ===== UPDATE GOAL (T042, T044) =====

    @Test // T042: same validations as POST apply
    void updateGoal_invalidTargetAmount_throwsInvalidTargetAmount() {
        when(savingsGoalRepository.findByGoalIdAndCustomerId(1L, 1L)).thenReturn(Optional.of(existingGoal));

        SavingsGoalRequest request = new SavingsGoalRequest("Travel", BigDecimal.ZERO, LocalDate.now().plusDays(30));

        assertThatThrownBy(() -> savingsGoalService.updateGoal(1L, 1L, request))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getCode())
                .isEqualTo("INVALID_TARGET_AMOUNT");
    }

    @Test // T044: progress_percentage and status recalculate after field change
    void updateGoal_newTargetAmount_progressRecalculates() {
        account.setBalance(new BigDecimal("1000.00"));
        when(savingsGoalRepository.findByGoalIdAndCustomerId(1L, 1L)).thenReturn(Optional.of(existingGoal));
        when(savingsGoalRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        doNothing().when(auditService).log(any(), any(), any(), any(), any(), any(), any(), any());

        // Change target from 5000 to 2000 (same 1000 balance → now 50%)
        SavingsGoalRequest request = new SavingsGoalRequest("Travel", new BigDecimal("2000.00"), LocalDate.now().plusDays(90));
        SavingsGoalResponse response = savingsGoalService.updateGoal(1L, 1L, request);

        // 1000 / 2000 * 100 = 50%
        assertThat(response.getProgressPercentage()).isEqualByComparingTo("50.00");
    }

    // ===== DELETE GOAL (T054) =====

    @Test // T054: soft delete sets deleted_at, row remains
    void deleteGoal_setsDeletedAt_doesNotHardDelete() {
        when(savingsGoalRepository.findByGoalIdAndCustomerId(1L, 1L)).thenReturn(Optional.of(existingGoal));
        when(savingsGoalRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        doNothing().when(auditService).log(any(), any(), any(), any(), any(), any(), any(), any());

        savingsGoalService.deleteGoal(1L, 1L);

        verify(savingsGoalRepository, never()).delete(any());
        verify(savingsGoalRepository, times(1)).save(argThat(g -> g.getDeletedAt() != null));
    }

    @Test // goal not found returns 404
    void deleteGoal_goalNotFound_throwsGoalNotFound() {
        when(savingsGoalRepository.findByGoalIdAndCustomerId(99L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> savingsGoalService.deleteGoal(1L, 99L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getCode())
                .isEqualTo("GOAL_NOT_FOUND");
    }

    // ===== GET ALL GOALS =====

    @Test
    void getAllGoalsForCustomer_returnsOnlyActiveGoals() {
        when(savingsGoalRepository.findAllActiveByCustomerId(1L)).thenReturn(List.of(existingGoal));

        List<SavingsGoalResponse> responses = savingsGoalService.getAllGoalsForCustomer(1L);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getGoalName()).isEqualTo("Travel");
    }

    @Test
    void getAllGoalsForCustomer_noGoals_returnsEmptyList() {
        when(savingsGoalRepository.findAllActiveByCustomerId(1L)).thenReturn(List.of());

        List<SavingsGoalResponse> responses = savingsGoalService.getAllGoalsForCustomer(1L);

        assertThat(responses).isEmpty();
    }
}
