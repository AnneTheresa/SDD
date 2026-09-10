package com.group1.banking.scheduler;

import com.group1.banking.entity.*;
import com.group1.banking.repository.AccountRepository;
import com.group1.banking.repository.IdempotencyRecordRepository;
import com.group1.banking.repository.StandingOrderRepository;
import com.group1.banking.repository.TransactionQueryRepository;
import com.group1.banking.service.AuditService;
import com.group1.banking.service.NotificationEvaluationService;
import com.group1.banking.util.CanadianHolidayService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class StandingOrderExecutionJobTest {

    @Mock
    private StandingOrderRepository standingOrderRepository;

    @Mock
    private TransactionQueryRepository transactionQueryRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private IdempotencyRecordRepository idempotencyRecordRepository;

    @Mock
    private NotificationEvaluationService notificationEvaluationService;

    @Mock
    private CanadianHolidayService canadianHolidayService;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private StandingOrderExecutionJob job;

    private StandingOrderEntity order;
    private Account sourceAccount;
    private Account payeeAccount;
    private Customer customer;

    @BeforeEach
    void setUp() {
        customer = new Customer();
        customer.setCustomerId(42L);

        sourceAccount = new Account();
        sourceAccount.setAccountId(1001L);
        sourceAccount.setBalance(new BigDecimal("1000.00"));
        sourceAccount.setCustomer(customer);
        sourceAccount.setStatus(AccountStatus.ACTIVE);
        sourceAccount.setAccountNumber("ACC0000001001");
        sourceAccount.setDailyTransferLimit(new BigDecimal("3000.00"));

        payeeAccount = new Account();
        payeeAccount.setAccountId(2002L);
        payeeAccount.setBalance(new BigDecimal("500.00"));
        payeeAccount.setCustomer(new Customer());
        payeeAccount.setStatus(AccountStatus.ACTIVE);
        payeeAccount.setAccountNumber("ACC0000002002");
        payeeAccount.setDailyTransferLimit(new BigDecimal("3000.00"));

        order = new StandingOrderEntity();
        order.setStandingOrderId("order-001");
        order.setSourceAccountId(1001L);
        order.setPayeeAccount(2002L);
        order.setPayeeName("John Payee");
        order.setAmount(new BigDecimal("100.00"));
        order.setFrequency(Frequency.MONTHLY);
        order.setStatus(StandingOrderStatus.ACTIVE);
        order.setNextRunDate(LocalDate.now().atStartOfDay());
        order.setReference("REF001");
        order.setStartDate(LocalDateTime.now().minusMonths(1));
    }

    // ===== processOrders() TESTS =====

    @Test
    void processOrders_shouldDoNothing_whenNoActiveOrders() {
        when(standingOrderRepository.findByStatusAndNextRunDateBefore(
                eq(StandingOrderStatus.ACTIVE), any())).thenReturn(List.of());

        job.processOrders();

        verify(idempotencyRecordRepository, never()).findByStorageKey(any());
        verify(accountRepository, never()).findById(any());
    }

    @Test
    void processOrders_shouldAttemptExecution_whenOrderIsDueToday() {
        order.setNextRunDate(LocalDate.now().atStartOfDay());

        when(standingOrderRepository.findByStatusAndNextRunDateBefore(
                eq(StandingOrderStatus.ACTIVE), any())).thenReturn(List.of(order));
        when(idempotencyRecordRepository.findByStorageKey(anyString())).thenReturn(Optional.empty());
        when(accountRepository.findById(1001L)).thenReturn(Optional.of(sourceAccount));
        when(accountRepository.findByAccountId(2002L)).thenReturn(Optional.of(payeeAccount));
        when(accountRepository.save(any())).thenReturn(sourceAccount);
        when(transactionQueryRepository.save(any())).thenReturn(new Transaction());
        when(idempotencyRecordRepository.save(any())).thenReturn(new IdempotencyRecord());
        when(standingOrderRepository.save(any())).thenReturn(order);
        when(canadianHolidayService.nextBusinessDay(any())).thenAnswer(inv -> inv.getArgument(0));
        doNothing().when(auditService).log(any(), any(), any(), any(), any(), any(), any(), any());

        job.processOrders();

        verify(transactionQueryRepository).save(any(Transaction.class));
    }

    @Test
    void processOrders_shouldSkipOrder_whenDateIsNotToday() {
        order.setNextRunDate(LocalDate.now().plusDays(5).atStartOfDay());

        when(standingOrderRepository.findByStatusAndNextRunDateBefore(
                eq(StandingOrderStatus.ACTIVE), any())).thenReturn(List.of(order));

        job.processOrders();

        verify(idempotencyRecordRepository, never()).findByStorageKey(any());
    }

    // ===== attemptExecution() TESTS =====

    @Test
    void attemptExecution_shouldSkip_whenIdempotencyKeyExists() {
        when(idempotencyRecordRepository.findByStorageKey(anyString()))
                .thenReturn(Optional.of(new IdempotencyRecord()));

        job.attemptExecution(order);

        verify(accountRepository, never()).findById(any());
        verify(transactionQueryRepository, never()).save(any());
    }

    @Test
    void attemptExecution_shouldSetTerminated_whenSourceAccountNotFound() {
        when(idempotencyRecordRepository.findByStorageKey(anyString())).thenReturn(Optional.empty());
        when(accountRepository.findById(1001L)).thenReturn(Optional.empty());
        when(standingOrderRepository.save(any())).thenReturn(order);

        job.attemptExecution(order);

        verify(standingOrderRepository).save(argThat(o -> o.getStatus() == StandingOrderStatus.TERMINATED));
    }

    @Test
    void attemptExecution_shouldSetRetryPending_whenInsufficientBalance() {
        sourceAccount.setBalance(new BigDecimal("50.00")); // less than order amount of 100.00
        when(idempotencyRecordRepository.findByStorageKey(anyString())).thenReturn(Optional.empty());
        when(accountRepository.findById(1001L)).thenReturn(Optional.of(sourceAccount));
        when(standingOrderRepository.save(any())).thenReturn(order);

        job.attemptExecution(order);

        verify(standingOrderRepository).save(argThat(o -> o.getStatus() == StandingOrderStatus.RETRY_PENDING));
    }

    @Test
    void attemptExecution_shouldExecuteTransfer_whenSufficientBalance() {
        when(idempotencyRecordRepository.findByStorageKey(anyString())).thenReturn(Optional.empty());
        when(accountRepository.findById(1001L)).thenReturn(Optional.of(sourceAccount));
        when(accountRepository.findByAccountId(2002L)).thenReturn(Optional.of(payeeAccount));
        when(accountRepository.save(any())).thenReturn(sourceAccount);
        when(transactionQueryRepository.save(any())).thenReturn(new Transaction());
        when(idempotencyRecordRepository.save(any())).thenReturn(new IdempotencyRecord());
        when(standingOrderRepository.save(any())).thenReturn(order);
        when(canadianHolidayService.nextBusinessDay(any())).thenAnswer(inv -> inv.getArgument(0));
        doNothing().when(auditService).log(any(), any(), any(), any(), any(), any(), any(), any());

        job.attemptExecution(order);

        // Source account balance should be reduced
        verify(accountRepository, atLeast(2)).save(any(Account.class));
        // Transaction should be saved
        verify(transactionQueryRepository).save(argThat(t -> t.getStatus() == TransactionStatus.SUCCESS));
    }

    @Test
    void attemptExecution_shouldDebitSourceAndCreditPayee_whenSuccess() {
        BigDecimal initialSourceBalance = sourceAccount.getBalance(); // 1000.00
        BigDecimal initialPayeeBalance = payeeAccount.getBalance();    // 500.00
        BigDecimal orderAmount = order.getAmount(); // 100.00

        when(idempotencyRecordRepository.findByStorageKey(anyString())).thenReturn(Optional.empty());
        when(accountRepository.findById(1001L)).thenReturn(Optional.of(sourceAccount));
        when(accountRepository.findByAccountId(2002L)).thenReturn(Optional.of(payeeAccount));
        when(accountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(transactionQueryRepository.save(any())).thenReturn(new Transaction());
        when(idempotencyRecordRepository.save(any())).thenReturn(new IdempotencyRecord());
        when(standingOrderRepository.save(any())).thenReturn(order);
        when(canadianHolidayService.nextBusinessDay(any())).thenAnswer(inv -> inv.getArgument(0));
        doNothing().when(auditService).log(any(), any(), any(), any(), any(), any(), any(), any());

        job.attemptExecution(order);

        assertThat(sourceAccount.getBalance()).isEqualByComparingTo(
                initialSourceBalance.subtract(orderAmount));
        assertThat(payeeAccount.getBalance()).isEqualByComparingTo(
                initialPayeeBalance.add(orderAmount));
    }

    @Test
    void attemptExecution_shouldAdvanceNextRunDateAfterSuccess() {
        LocalDateTime originalNextRun = order.getNextRunDate();
        when(idempotencyRecordRepository.findByStorageKey(anyString())).thenReturn(Optional.empty());
        when(accountRepository.findById(1001L)).thenReturn(Optional.of(sourceAccount));
        when(accountRepository.findByAccountId(2002L)).thenReturn(Optional.of(payeeAccount));
        when(accountRepository.save(any())).thenReturn(sourceAccount);
        when(transactionQueryRepository.save(any())).thenReturn(new Transaction());
        when(idempotencyRecordRepository.save(any())).thenReturn(new IdempotencyRecord());
        when(canadianHolidayService.nextBusinessDay(any())).thenAnswer(inv -> {
            LocalDateTime next = inv.getArgument(0);
            return next.plusMonths(1); // simulate advancing
        });
        when(standingOrderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        doNothing().when(auditService).log(any(), any(), any(), any(), any(), any(), any(), any());

        job.attemptExecution(order);

        // After execution, status should be ACTIVE and nextRunDate advanced
        verify(standingOrderRepository).save(argThat(o ->
                o.getStatus() == StandingOrderStatus.ACTIVE &&
                !o.getNextRunDate().equals(originalNextRun)));
    }

    @Test
    void attemptExecution_shouldStoreIdempotencyRecord_onSuccess() {
        when(idempotencyRecordRepository.findByStorageKey(anyString())).thenReturn(Optional.empty());
        when(accountRepository.findById(1001L)).thenReturn(Optional.of(sourceAccount));
        when(accountRepository.findByAccountId(2002L)).thenReturn(Optional.of(payeeAccount));
        when(accountRepository.save(any())).thenReturn(sourceAccount);
        when(transactionQueryRepository.save(any())).thenReturn(new Transaction());
        when(canadianHolidayService.nextBusinessDay(any())).thenAnswer(inv -> inv.getArgument(0));
        when(standingOrderRepository.save(any())).thenReturn(order);
        doNothing().when(auditService).log(any(), any(), any(), any(), any(), any(), any(), any());

        job.attemptExecution(order);

        verify(idempotencyRecordRepository).save(argThat(r ->
                r.getOperationType().equals("STANDING_ORDER_EXECUTE") &&
                r.getResponseStatus() == 200));
    }

    // ===== firstAttempt() TESTS =====

    @Test
    void firstAttempt_shouldDoNothing_whenNoRetryPendingOrders() {
        when(standingOrderRepository.findByStatus(StandingOrderStatus.RETRY_PENDING))
                .thenReturn(List.of());

        job.firstAttempt();

        verify(accountRepository, never()).findById(any());
    }

    @Test
    void firstAttempt_shouldAttemptExecution_whenOrderIsDueToday() {
        order.setStatus(StandingOrderStatus.RETRY_PENDING);
        order.setNextRunDate(LocalDate.now().atStartOfDay());

        when(standingOrderRepository.findByStatus(StandingOrderStatus.RETRY_PENDING))
                .thenReturn(List.of(order));
        when(idempotencyRecordRepository.findByStorageKey(anyString())).thenReturn(Optional.empty());
        when(accountRepository.findById(1001L)).thenReturn(Optional.of(sourceAccount));
        when(accountRepository.findByAccountId(2002L)).thenReturn(Optional.of(payeeAccount));
        when(accountRepository.save(any())).thenReturn(sourceAccount);
        when(transactionQueryRepository.save(any())).thenReturn(new Transaction());
        when(idempotencyRecordRepository.save(any())).thenReturn(new IdempotencyRecord());
        when(standingOrderRepository.save(any())).thenReturn(order);
        when(canadianHolidayService.nextBusinessDay(any())).thenAnswer(inv -> inv.getArgument(0));
        doNothing().when(auditService).log(any(), any(), any(), any(), any(), any(), any(), any());

        job.firstAttempt();

        verify(transactionQueryRepository).save(any(Transaction.class));
    }

    @Test
    void firstAttempt_shouldSkipOrder_whenNotDueToday() {
        order.setStatus(StandingOrderStatus.RETRY_PENDING);
        order.setNextRunDate(LocalDate.now().minusDays(1).atStartOfDay()); // yesterday

        when(standingOrderRepository.findByStatus(StandingOrderStatus.RETRY_PENDING))
                .thenReturn(List.of(order));

        job.firstAttempt();

        verify(idempotencyRecordRepository, never()).findByStorageKey(any());
    }

    // ===== finalAttempt() TESTS =====

    @Test
    void finalAttempt_shouldDoNothing_whenNoRetryPendingOrders() {
        when(standingOrderRepository.findByStatus(StandingOrderStatus.RETRY_PENDING))
                .thenReturn(List.of());

        job.finalAttempt();

        verify(accountRepository, never()).findById(any());
    }

    @Test
    void finalAttempt_shouldSetFailedInsufficientFunds_whenBalanceStillInsufficient() {
        order.setStatus(StandingOrderStatus.RETRY_PENDING);
        order.setNextRunDate(LocalDate.now().atStartOfDay());
        sourceAccount.setBalance(new BigDecimal("10.00")); // way below 100.00

        when(standingOrderRepository.findByStatus(StandingOrderStatus.RETRY_PENDING))
                .thenReturn(List.of(order));
        when(accountRepository.findById(1001L)).thenReturn(Optional.of(sourceAccount));
        when(transactionQueryRepository.save(any())).thenReturn(new Transaction());
        when(standingOrderRepository.save(any())).thenReturn(order);
        doNothing().when(notificationEvaluationService).evaluateInternal(
                anyString(), anyLong(), anyLong(), anyString(), anyString());
        doNothing().when(auditService).log(any(), any(), any(), any(), any(), any(), any(), any());

        job.finalAttempt();

        verify(standingOrderRepository).save(argThat(o ->
                o.getStatus() == StandingOrderStatus.FAILED_INSUFFICIENT_FUNDS));
        verify(transactionQueryRepository).save(argThat(t -> t.getStatus() == TransactionStatus.FAILED));
    }

    @Test
    void finalAttempt_shouldExecuteSuccessfully_whenBalanceSufficient() {
        order.setStatus(StandingOrderStatus.RETRY_PENDING);
        order.setNextRunDate(LocalDate.now().atStartOfDay());
        // sourceAccount has 1000.00, order.amount is 100.00

        when(standingOrderRepository.findByStatus(StandingOrderStatus.RETRY_PENDING))
                .thenReturn(List.of(order));
        when(accountRepository.findById(1001L)).thenReturn(Optional.of(sourceAccount));
        when(accountRepository.findByAccountId(2002L)).thenReturn(Optional.of(payeeAccount));
        when(idempotencyRecordRepository.findByStorageKey(anyString())).thenReturn(Optional.empty());
        when(accountRepository.save(any())).thenReturn(sourceAccount);
        when(transactionQueryRepository.save(any())).thenReturn(new Transaction());
        when(idempotencyRecordRepository.save(any())).thenReturn(new IdempotencyRecord());
        when(standingOrderRepository.save(any())).thenReturn(order);
        when(canadianHolidayService.nextBusinessDay(any())).thenAnswer(inv -> inv.getArgument(0));
        doNothing().when(auditService).log(any(), any(), any(), any(), any(), any(), any(), any());

        job.finalAttempt();

        verify(transactionQueryRepository).save(argThat(t -> t.getStatus() == TransactionStatus.SUCCESS));
    }

    @Test
    void finalAttempt_shouldSetTerminated_whenSourceAccountNotFound() {
        order.setStatus(StandingOrderStatus.RETRY_PENDING);
        order.setNextRunDate(LocalDate.now().atStartOfDay());

        when(standingOrderRepository.findByStatus(StandingOrderStatus.RETRY_PENDING))
                .thenReturn(List.of(order));
        when(accountRepository.findById(1001L)).thenReturn(Optional.empty());
        when(standingOrderRepository.save(any())).thenReturn(order);

        job.finalAttempt();

        verify(standingOrderRepository).save(argThat(o -> o.getStatus() == StandingOrderStatus.TERMINATED));
    }

    @Test
    void finalAttempt_shouldTriggerNotification_whenFinalFailure() {
        order.setStatus(StandingOrderStatus.RETRY_PENDING);
        order.setNextRunDate(LocalDate.now().atStartOfDay());
        sourceAccount.setBalance(new BigDecimal("5.00")); // insufficient

        when(standingOrderRepository.findByStatus(StandingOrderStatus.RETRY_PENDING))
                .thenReturn(List.of(order));
        when(accountRepository.findById(1001L)).thenReturn(Optional.of(sourceAccount));
        when(transactionQueryRepository.save(any())).thenReturn(new Transaction());
        when(standingOrderRepository.save(any())).thenReturn(order);
        doNothing().when(notificationEvaluationService).evaluateInternal(
                anyString(), anyLong(), anyLong(), anyString(), anyString());
        doNothing().when(auditService).log(any(), any(), any(), any(), any(), any(), any(), any());

        job.finalAttempt();

        verify(notificationEvaluationService).evaluateInternal(
                eq("StandingOrderFailure"), anyLong(), anyLong(), anyString(), anyString());
    }

    @Test
    void finalAttempt_shouldHandleNotificationException_gracefully() {
        order.setStatus(StandingOrderStatus.RETRY_PENDING);
        order.setNextRunDate(LocalDate.now().atStartOfDay());
        sourceAccount.setBalance(new BigDecimal("5.00")); // insufficient

        when(standingOrderRepository.findByStatus(StandingOrderStatus.RETRY_PENDING))
                .thenReturn(List.of(order));
        when(accountRepository.findById(1001L)).thenReturn(Optional.of(sourceAccount));
        when(transactionQueryRepository.save(any())).thenReturn(new Transaction());
        when(standingOrderRepository.save(any())).thenReturn(order);
        doThrow(new RuntimeException("Notification service down"))
                .when(notificationEvaluationService).evaluateInternal(
                        anyString(), anyLong(), anyLong(), anyString(), anyString());
        doNothing().when(auditService).log(any(), any(), any(), any(), any(), any(), any(), any());

        // Should not throw — exception is caught gracefully
        job.finalAttempt();

        verify(standingOrderRepository).save(argThat(o ->
                o.getStatus() == StandingOrderStatus.FAILED_INSUFFICIENT_FUNDS));
    }

    // ===== Frequency calculation TESTS (via attemptExecution) =====

    @Test
    void attemptExecution_shouldAdvanceByWeek_whenFrequencyIsWeekly() {
        order.setFrequency(Frequency.WEEKLY);
        order.setNextRunDate(LocalDate.now().atStartOfDay());

        when(idempotencyRecordRepository.findByStorageKey(anyString())).thenReturn(Optional.empty());
        when(accountRepository.findById(1001L)).thenReturn(Optional.of(sourceAccount));
        when(accountRepository.findByAccountId(2002L)).thenReturn(Optional.of(payeeAccount));
        when(accountRepository.save(any())).thenReturn(sourceAccount);
        when(transactionQueryRepository.save(any())).thenReturn(new Transaction());
        when(idempotencyRecordRepository.save(any())).thenReturn(new IdempotencyRecord());
        when(canadianHolidayService.nextBusinessDay(any())).thenAnswer(inv -> inv.getArgument(0));
        when(standingOrderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        doNothing().when(auditService).log(any(), any(), any(), any(), any(), any(), any(), any());

        job.attemptExecution(order);

        verify(standingOrderRepository).save(argThat(o ->
                o.getNextRunDate().toLocalDate().equals(
                        LocalDate.now().plusWeeks(1))));
    }

    @Test
    void attemptExecution_shouldAdvanceByDay_whenFrequencyIsDaily() {
        order.setFrequency(Frequency.DAILY);
        order.setNextRunDate(LocalDate.now().atStartOfDay());

        when(idempotencyRecordRepository.findByStorageKey(anyString())).thenReturn(Optional.empty());
        when(accountRepository.findById(1001L)).thenReturn(Optional.of(sourceAccount));
        when(accountRepository.findByAccountId(2002L)).thenReturn(Optional.of(payeeAccount));
        when(accountRepository.save(any())).thenReturn(sourceAccount);
        when(transactionQueryRepository.save(any())).thenReturn(new Transaction());
        when(idempotencyRecordRepository.save(any())).thenReturn(new IdempotencyRecord());
        when(canadianHolidayService.nextBusinessDay(any())).thenAnswer(inv -> inv.getArgument(0));
        when(standingOrderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        doNothing().when(auditService).log(any(), any(), any(), any(), any(), any(), any(), any());

        job.attemptExecution(order);

        verify(standingOrderRepository).save(argThat(o ->
                o.getNextRunDate().toLocalDate().equals(LocalDate.now().plusDays(1))));
    }

    @Test
    void attemptExecution_shouldAdvanceByQuarter_whenFrequencyIsQuarterly() {
        order.setFrequency(Frequency.QUARTERLY);
        order.setNextRunDate(LocalDate.now().atStartOfDay());

        when(idempotencyRecordRepository.findByStorageKey(anyString())).thenReturn(Optional.empty());
        when(accountRepository.findById(1001L)).thenReturn(Optional.of(sourceAccount));
        when(accountRepository.findByAccountId(2002L)).thenReturn(Optional.of(payeeAccount));
        when(accountRepository.save(any())).thenReturn(sourceAccount);
        when(transactionQueryRepository.save(any())).thenReturn(new Transaction());
        when(idempotencyRecordRepository.save(any())).thenReturn(new IdempotencyRecord());
        when(canadianHolidayService.nextBusinessDay(any())).thenAnswer(inv -> inv.getArgument(0));
        when(standingOrderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        doNothing().when(auditService).log(any(), any(), any(), any(), any(), any(), any(), any());

        job.attemptExecution(order);

        verify(standingOrderRepository).save(argThat(o ->
                o.getNextRunDate().toLocalDate().equals(
                        LocalDate.now().plusMonths(3))));
    }
}
