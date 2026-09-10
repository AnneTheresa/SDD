package com.group1.banking.controller;

import tools.jackson.databind.json.JsonMapper;
import com.group1.banking.dto.request.NotificationEventRequest;
import com.group1.banking.dto.response.NotificationDecisionResponse;
import com.group1.banking.repository.UserRepository;
import com.group1.banking.security.JwtService;
import com.group1.banking.service.NotificationEvaluationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Controller tests for NotificationController.
 */
@WebMvcTest(NotificationController.class)
@AutoConfigureMockMvc(addFilters = false)
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper objectMapper;

    @MockitoBean
    private NotificationEvaluationService notificationEvaluationService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserRepository userRepository;

    private NotificationDecisionResponse sampleDecision(String decision) {
        NotificationDecisionResponse r = new NotificationDecisionResponse();
        r.setEventId("evt-001");
        r.setDecision(decision);
        r.setDecisionReason("Mandatory event");
        r.setAccountId(1001L);
        r.setCustomerId(42L);
        r.setEvaluatedAt(LocalDateTime.now());
        r.setMandatoryOverride(false);
        return r;
    }

    private NotificationEventRequest validRequest(String eventType) {
        NotificationEventRequest req = new NotificationEventRequest();
        req.setEventId("evt-001");
        req.setEventType(eventType);
        req.setAccountId(1001L);
        req.setCustomerId(42L);
        req.setBusinessTimestamp("2026-04-21T10:00:00Z");
        return req;
    }

    @Test
    void evaluate_shouldReturn200_forMandatoryEvent() throws Exception {
        when(notificationEvaluationService.evaluate(any(NotificationEventRequest.class)))
                .thenReturn(sampleDecision("RAISED"));

        mockMvc.perform(post("/notifications/evaluate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest("StandingOrderFailure"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventId").value("evt-001"))
                .andExpect(jsonPath("$.decision").value("RAISED"));
    }

    @Test
    void evaluate_shouldReturn200_whenDecisionIsSuppressed() throws Exception {
        when(notificationEvaluationService.evaluate(any(NotificationEventRequest.class)))
                .thenReturn(sampleDecision("SUPPRESSED"));

        mockMvc.perform(post("/notifications/evaluate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest("StandingOrderCreation"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.decision").value("SUPPRESSED"));
    }

    @Test
    void evaluate_shouldReturn400_whenEventIdMissing() throws Exception {
        NotificationEventRequest req = new NotificationEventRequest();
        // eventId missing
        req.setEventType("StandingOrderFailure");
        req.setAccountId(1001L);
        req.setCustomerId(42L);
        req.setBusinessTimestamp("2026-04-21T10:00:00Z");

        mockMvc.perform(post("/notifications/evaluate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnprocessableContent());
    }

    @Test
    void evaluate_shouldReturn400_whenEventTypeMissing() throws Exception {
        NotificationEventRequest req = new NotificationEventRequest();
        req.setEventId("evt-001");
        // eventType missing
        req.setAccountId(1001L);
        req.setCustomerId(42L);
        req.setBusinessTimestamp("2026-04-21T10:00:00Z");

        mockMvc.perform(post("/notifications/evaluate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnprocessableContent());
    }

    @Test
    void evaluate_shouldReturn400_whenAccountIdMissing() throws Exception {
        NotificationEventRequest req = new NotificationEventRequest();
        req.setEventId("evt-001");
        req.setEventType("StandingOrderFailure");
        // accountId missing
        req.setCustomerId(42L);
        req.setBusinessTimestamp("2026-04-21T10:00:00Z");

        mockMvc.perform(post("/notifications/evaluate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnprocessableContent());
    }

    @Test
    void evaluate_shouldReturn400_whenTimestampMissing() throws Exception {
        NotificationEventRequest req = new NotificationEventRequest();
        req.setEventId("evt-001");
        req.setEventType("StandingOrderFailure");
        req.setAccountId(1001L);
        req.setCustomerId(42L);
        // businessTimestamp missing

        mockMvc.perform(post("/notifications/evaluate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnprocessableContent());
    }
}
