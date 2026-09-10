package com.group1.banking.service;

import com.group1.banking.config.RiskScoreRules;
import com.group1.banking.dto.RiskScoreResponse;
import com.group1.banking.entity.Customer;
import com.group1.banking.enums.RiskScoreStatus;
import com.group1.banking.mapper.RiskScoreMapper;
import com.group1.banking.repository.AccountRepository;
import com.group1.banking.repository.CustomerRepository;
import com.group1.banking.repository.RiskScoreRepository;
import com.group1.banking.repository.TransactionRepository;
import com.group1.banking.service.impl.RiskScoreService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.io.ClassPathResource;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;

import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Insufficient-data handling for risk scoring.
 *
 * Loads the real rules from risk-score-rules.yaml so the minMonths threshold
 * under test is the configured one rather than a value duplicated here.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RiskScoreServiceInsufficientDataTest {

    private static final Long CUSTOMER_ID = 900001L;

    private RiskScoreRules riskScoreRules;

    @Mock
    private RiskScoreRepository riskScoreRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private SavingsGoalService savingsGoalService;

    private RiskScoreService riskScoreService;

    /**
     * Binds the real risk-score-rules.yaml rather than hand-rolling a fixture, so
     * the thresholds under test stay in step with the shipped configuration.
     */
    private static RiskScoreRules loadRealRules() throws IOException {
        List<PropertySource<?>> sources = new YamlPropertySourceLoader()
                .load("risk-score-rules", new ClassPathResource("risk-score-rules.yaml"));

        StandardEnvironment environment = new StandardEnvironment();
        sources.forEach(source -> environment.getPropertySources().addLast(source));

        return Binder.get(environment)
                .bind("risk-score", RiskScoreRules.class)
                .orElseThrow(() -> new IllegalStateException("risk-score-rules.yaml failed to bind"));
    }

    @BeforeEach
    void setUp() throws IOException {
        riskScoreRules = loadRealRules();

        JsonMapper jsonMapper = JsonMapper.builder().build();
        riskScoreService = new RiskScoreService(riskScoreRepository, customerRepository, accountRepository,
                transactionRepository, savingsGoalService, riskScoreRules, jsonMapper,
                new RiskScoreMapper(riskScoreRules, jsonMapper));

        Customer customer = new Customer();
        customer.setCustomerId(CUSTOMER_ID);
        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer));

        when(accountRepository.findAllByCustomerCustomerIdAndDeletedAtIsNullAndStatus(anyLong(), any()))
                .thenReturn(List.of());
        when(transactionRepository
                .findByAccount_Customer_CustomerIdAndTimestampBetweenOrderByTimestampAsc(anyLong(), any(), any()))
                .thenReturn(List.of());
        when(savingsGoalService.getAllGoalsForCustomer(anyLong())).thenReturn(List.of());
    }

    private Instant monthsAgo(int months) {
        return LocalDate.now(ZoneOffset.UTC).minusMonths(months).atStartOfDay().toInstant(ZoneOffset.UTC);
    }

    @Test
    void returnsInsufficientDataWhenCustomerHasNoTransactions() {
        when(transactionRepository.findEarliestTimestampForCustomer(CUSTOMER_ID)).thenReturn(null);

        RiskScoreResponse response = riskScoreService.calculateRiskScore(CUSTOMER_ID);

        assertThat(response.getCalculateStatus()).isEqualTo(RiskScoreStatus.INSUFFICIENT_DATA);
    }

    @Test
    void returnsInsufficientDataWhenHistoryIsShorterThanMinMonths() {
        when(transactionRepository.findEarliestTimestampForCustomer(CUSTOMER_ID)).thenReturn(monthsAgo(1));

        RiskScoreResponse response = riskScoreService.calculateRiskScore(CUSTOMER_ID);

        assertThat(response.getCalculateStatus()).isEqualTo(RiskScoreStatus.INSUFFICIENT_DATA);
    }

    /**
     * Scenario 1: no score or band is fabricated. Without the guard the neutral
     * factor defaults would score an empty history as LOW.
     */
    @Test
    void doesNotFabricateAScoreOrBand() {
        when(transactionRepository.findEarliestTimestampForCustomer(CUSTOMER_ID)).thenReturn(null);

        RiskScoreResponse response = riskScoreService.calculateRiskScore(CUSTOMER_ID);

        assertThat(response.getScore()).isNull();
        assertThat(response.getLevel()).isNull();
        assertThat(response.getOverAllExplain()).isNull();
    }

    /**
     * Scenario 2: null score and band are omitted from the serialized payload
     * rather than emitted as explicit nulls.
     */
    @Test
    void omitsScoreAndBandFromSerializedPayload() {
        when(transactionRepository.findEarliestTimestampForCustomer(CUSTOMER_ID)).thenReturn(null);

        RiskScoreResponse response = riskScoreService.calculateRiskScore(CUSTOMER_ID);
        String json = JsonMapper.builder().build().writeValueAsString(response);

        assertThat(json).doesNotContain("\"score\"");
        assertThat(json).doesNotContain("\"level\"");
        assertThat(json).contains("\"status\":\"INSUFFICIENT_DATA\"");
    }

    /**
     * Scenario 3: identified by a stable code, not an ad-hoc string.
     */
    @Test
    void surfacesAStableErrorCode() {
        when(transactionRepository.findEarliestTimestampForCustomer(CUSTOMER_ID)).thenReturn(null);

        RiskScoreResponse response = riskScoreService.calculateRiskScore(CUSTOMER_ID);

        assertThat(response.getCode()).isEqualTo("RISK_SCORE_INSUFFICIENT_DATA");
        assertThat(response.getMessage()).isNotBlank();
    }

    /**
     * Nothing is persisted when there is no score to record.
     */
    @Test
    void doesNotPersistAnyRiskScore() {
        when(transactionRepository.findEarliestTimestampForCustomer(CUSTOMER_ID)).thenReturn(null);

        riskScoreService.calculateRiskScore(CUSTOMER_ID);

        verify(riskScoreRepository, never()).save(any());
    }

    /**
     * Scenario 4: once the history is long enough the normal path resumes and a
     * real score, band and explanation come back.
     */
    @Test
    void producesANormalResultOnceHistoryIsLongEnough() {
        int minMonths = riskScoreRules.getInsufficientConditions().getMinMonths();
        when(transactionRepository.findEarliestTimestampForCustomer(CUSTOMER_ID))
                .thenReturn(monthsAgo(minMonths + 1));

        RiskScoreResponse response = riskScoreService.calculateRiskScore(CUSTOMER_ID);

        assertThat(response.getCalculateStatus()).isEqualTo(RiskScoreStatus.OK);
        assertThat(response.getScore()).isNotNull();
        assertThat(response.getLevel()).isNotNull();
        assertThat(response.getOverAllExplain()).isNotBlank();
        assertThat(response.getCode()).isNull();
        verify(riskScoreRepository).save(any());
    }
}
