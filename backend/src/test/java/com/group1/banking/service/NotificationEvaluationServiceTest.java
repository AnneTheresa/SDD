package com.group1.banking.service;

import com.group1.banking.dto.request.NotificationEventRequest;
import com.group1.banking.dto.response.NotificationDecisionResponse;
import com.group1.banking.entity.*;
import com.group1.banking.exception.BusinessStateException;
import com.group1.banking.exception.SemanticValidationException;
import com.group1.banking.mapper.NotificationDecisionMapper;
import com.group1.banking.repository.AccountRepository;
import com.group1.banking.repository.NotificationDecisionRepository;
import com.group1.banking.repository.NotificationPreferenceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for NotificationEvaluationService.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class NotificationEvaluationServiceTest {

    @Mock
    private NotificationDecisionRepository notificationDecisionRepository;

    @Mock
    private NotificationPreferenceRepository notificationPreferenceRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private NotificationDecisionMapper mapper;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private NotificationEvaluationService service;

    private Account account;
    private Customer customer;
    private NotificationEventRequest request;
    private NotificationDecisionResponse response;

    @BeforeEach
    void setUp() {
        customer = new Customer();
        customer.setCustomerId(42L);

        account = new Account();
        account.setAccountId(100L);
        account.setCustomer(customer);

        response = new NotificationDecisionResponse();
        // Default happy-path stubs
        when(notificationDecisionRepository.existsByEventId(anyString())).thenReturn(false);
        when(accountRepository.findById(100L)).thenReturn(Optional.of(account));
        when(notificationDecisionRepository.save(any())).thenReturn(new NotificationDecisionEntity());
        when(mapper.toResponse(any())).thenReturn(response);
        doNothing().when(auditService).log(any(), any(), any(), any(), any(), any(), any(), any());

        request = buildRequest("evt-001", "StandingOrderFailure", 100L, 42L);
    }

    private NotificationEventRequest buildRequest(String eventId, String eventType,
                                                   Long accountId, Long customerId) {
        NotificationEventRequest req = new NotificationEventRequest();
        req.setEventId(eventId);
        req.setEventType(eventType);
        req.setAccountId(accountId);
        req.setCustomerId(customerId);
        req.setBusinessTimestamp("2024-06-15T10:00:00");
        req.setPayload("Test payload");
        return req;
    }

    // ===== evaluate() — MANDATORY EVENTS =====

    @Test
    void evaluate_shouldReturnRAISED_whenMandatoryEventStandingOrderFailure() {
        when(notificationPreferenceRepository.findByCustomerIdAndEventType(42L, "StandingOrderFailure"))
                .thenReturn(Optional.empty());

        NotificationDecisionResponse result = service.evaluate(request);

        assertThat(result).isNotNull();
        verify(notificationDecisionRepository).save(argThat(e ->
                e.getDecision() == NotificationDecision.RAISED));
    }

    @Test
    void evaluate_shouldSetMandatoryOverride_whenCustomerOptedOut() {
        NotificationPreferenceEntity pref = new NotificationPreferenceEntity();
        pref.setCustomerId(42L);
        pref.setEventType("StandingOrderFailure");
        pref.setOptedIn(false);
        when(notificationPreferenceRepository.findByCustomerIdAndEventType(42L, "StandingOrderFailure"))
                .thenReturn(Optional.of(pref));

        service.evaluate(request);

        verify(notificationDecisionRepository).save(argThat(e ->
                e.isMandatoryOverride() && e.getDecision() == NotificationDecision.RAISED));
    }

    @Test
    void evaluate_shouldReturnRAISED_whenUnusualAccountActivityEvent() {
        NotificationEventRequest req = buildRequest("evt-002", "UnusualAccountActivity", 100L, 42L);
        when(notificationPreferenceRepository.findByCustomerIdAndEventType(42L, "UnusualAccountActivity"))
                .thenReturn(Optional.empty());

        service.evaluate(req);

        verify(notificationDecisionRepository).save(argThat(e ->
                e.getDecision() == NotificationDecision.RAISED && !e.isMandatoryOverride()));
    }

    // ===== evaluate() — OPTIONAL EVENTS =====

    @Test
    void evaluate_shouldReturnSUPPRESSED_whenOptionalAndCustomerOptedOut() {
        NotificationEventRequest req = buildRequest("evt-003", "StatementAvailability", 100L, 42L);
        NotificationPreferenceEntity pref = new NotificationPreferenceEntity();
        pref.setOptedIn(false);
        when(notificationPreferenceRepository.findByCustomerIdAndEventType(42L, "StatementAvailability"))
                .thenReturn(Optional.of(pref));

        service.evaluate(req);

        verify(notificationDecisionRepository).save(argThat(e ->
                e.getDecision() == NotificationDecision.SUPPRESSED));
    }

    @Test
    void evaluate_shouldReturnRAISED_whenOptionalAndCustomerOptedIn() {
        NotificationEventRequest req = buildRequest("evt-004", "StandingOrderCreation", 100L, 42L);
        NotificationPreferenceEntity pref = new NotificationPreferenceEntity();
        pref.setOptedIn(true);
        when(notificationPreferenceRepository.findByCustomerIdAndEventType(42L, "StandingOrderCreation"))
                .thenReturn(Optional.of(pref));

        service.evaluate(req);

        verify(notificationDecisionRepository).save(argThat(e ->
                e.getDecision() == NotificationDecision.RAISED));
    }

    @Test
    void evaluate_shouldReturnRAISED_whenOptionalAndNoPreferenceSet() {
        NotificationEventRequest req = buildRequest("evt-005", "StatementAvailability", 100L, 42L);
        when(notificationPreferenceRepository.findByCustomerIdAndEventType(42L, "StatementAvailability"))
                .thenReturn(Optional.empty());

        service.evaluate(req);

        verify(notificationDecisionRepository).save(argThat(e ->
                e.getDecision() == NotificationDecision.RAISED));
    }

    // ===== evaluate() — ERROR CASES =====

    @Test
    void evaluate_shouldThrow_whenDuplicateEventId() {
        when(notificationDecisionRepository.existsByEventId("evt-001")).thenReturn(true);

        assertThatThrownBy(() -> service.evaluate(request))
                .isInstanceOf(BusinessStateException.class);
    }

    @Test
    void evaluate_shouldThrow_whenUnknownEventType() {
        NotificationEventRequest req = buildRequest("evt-006", "INVALID_EVENT", 100L, 42L);

        assertThatThrownBy(() -> service.evaluate(req))
                .isInstanceOf(SemanticValidationException.class);
    }

    @Test
    void evaluate_shouldThrow_whenAccountNotFound() {
        when(accountRepository.findById(100L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.evaluate(request))
                .isInstanceOf(SemanticValidationException.class);
    }

    @Test
    void evaluate_shouldThrow_whenCustomerNotLinkedToAccount() {
        NotificationEventRequest req = buildRequest("evt-007", "StandingOrderFailure", 100L, 999L);

        assertThatThrownBy(() -> service.evaluate(req))
                .isInstanceOf(SemanticValidationException.class);
    }

    @Test
    void evaluate_shouldHandleInvalidTimestamp_gracefully() {
        request.setBusinessTimestamp("not-a-valid-timestamp");
        when(notificationPreferenceRepository.findByCustomerIdAndEventType(42L, "StandingOrderFailure"))
                .thenReturn(Optional.empty());

        // Should not throw — falls back to LocalDateTime.now()
        NotificationDecisionResponse result = service.evaluate(request);
        assertThat(result).isNotNull();
    }

    @Test
    void evaluate_shouldPersistEventDetails() {
        when(notificationPreferenceRepository.findByCustomerIdAndEventType(42L, "StandingOrderFailure"))
                .thenReturn(Optional.empty());

        service.evaluate(request);

        verify(notificationDecisionRepository).save(argThat(e ->
                e.getEventId().equals("evt-001") &&
                e.getEventType().equals("StandingOrderFailure") &&
                e.getAccountId().equals(100L) &&
                e.getCustomerId().equals(42L)));
    }

    // ===== evaluateInternal() TESTS =====

    @Test
    void evaluateInternal_shouldCallEvaluate_withGeneratedEventId() {
        when(notificationPreferenceRepository.findByCustomerIdAndEventType(42L, "StandingOrderFailure"))
                .thenReturn(Optional.empty());

        service.evaluateInternal("StandingOrderFailure", 100L, 42L, "2024-06-15T10:00:00", "payload");

        verify(notificationDecisionRepository).save(argThat(e ->
                e.getEventType().equals("StandingOrderFailure") && e.getEventId() != null));
    }

    @Test
    void evaluateInternal_shouldNotPropagateException_whenEvaluateFails() {
        // Force a failure in evaluate — unknown event type
        // Should be caught and logged internally
        service.evaluateInternal("INVALID_TYPE", 100L, 42L, "2024-06-15T10:00:00", "payload");

        // No exception propagated — verify auditService was called for the error
        verify(auditService, atLeastOnce()).log(any(), any(), any(), any(), any(), any(), any(), any());
    }
}
