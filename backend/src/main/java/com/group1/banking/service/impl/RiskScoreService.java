package com.group1.banking.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.group1.banking.config.RiskScoreRules;
import com.group1.banking.config.RiskScoreRules.Band;
import com.group1.banking.config.RiskScoreRules.FactorConfig;
import com.group1.banking.config.RiskScoreRules.RiskScoreBand;
import com.group1.banking.dto.RiskScoreFactor;
import com.group1.banking.dto.RiskScoreResponse;
import com.group1.banking.dto.SavingsGoalResponse;
import com.group1.banking.entity.Account;
import com.group1.banking.entity.AccountStatus;
import com.group1.banking.entity.Customer;
import com.group1.banking.entity.RiskScore;
import com.group1.banking.entity.Transaction;
import com.group1.banking.entity.TransactionDirection;
import com.group1.banking.entity.TransactionStatus;
import com.group1.banking.enums.RiskScoreDataElement;
import com.group1.banking.enums.RiskScoreLevel;
import com.group1.banking.enums.RiskScoreStatus;
import com.group1.banking.exception.NotFoundException;
import com.group1.banking.mapper.RiskScoreMapper;
import com.group1.banking.repository.AccountRepository;
import com.group1.banking.repository.CustomerRepository;
import com.group1.banking.repository.RiskScoreRepository;
import com.group1.banking.repository.TransactionRepository;
import com.group1.banking.security.CustomUserPrincipal;
import com.group1.banking.service.SavingsGoalService;

import lombok.extern.slf4j.Slf4j;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

@Service
@Slf4j
public class RiskScoreService {

    private final RiskScoreRepository riskScoreRepository;
    private final CustomerRepository customerRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final SavingsGoalService savingsGoalService;
    private final JsonMapper objectMapper;
    private final RiskScoreMapper riskScoreMapper;

    private final RiskScoreRules riskScoreRules;

    private final long LOOK_BACK_MONTH = 3;
    private final double MAX_RATIO = 999.0;
    private final double MIN_RATIO = 0.0;
    private final double MAX_COVER = 999;

    public RiskScoreService(RiskScoreRepository riskScoreRepository, CustomerRepository customerRepository,
            AccountRepository accountRepository, TransactionRepository transactionRepository,
            SavingsGoalService savingsGoalService, RiskScoreRules riskScoreRules, JsonMapper objectMapper,
            RiskScoreMapper riskScoreMapper) {
        this.riskScoreRepository = riskScoreRepository;
        this.customerRepository = customerRepository;
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.savingsGoalService = savingsGoalService;
        this.riskScoreRules = riskScoreRules;
        this.objectMapper = objectMapper;
        this.riskScoreMapper = riskScoreMapper;
    }

    @Transactional
    public RiskScoreResponse calculateRiskScore(Long customer_id) {
        RiskScore riskScoreEntity = buildRiskScore(customer_id);

        if (riskScoreEntity == null) {
            return buildInsufficientDataResponse(customer_id);
        }

        riskScoreRepository.save(riskScoreEntity);

        return riskScoreMapper.toResponse(riskScoreEntity);
    }

    @Transactional(readOnly = true)
    public RiskScoreResponse calculateRiskScoreWithoutSaving(Long customer_id) {
        RiskScore riskScoreEntity = buildRiskScore(customer_id);

        if (riskScoreEntity == null) {
            return buildInsufficientDataResponse(customer_id);
        }

        return riskScoreMapper.toResponse(riskScoreEntity);
    }

    private RiskScore buildRiskScore(Long customer_id) {
        Customer customer = this.customerRepository.findById(customer_id)
                .orElseThrow(
                        () -> new NotFoundException("CUSTOMER_NOT_FOUND", "Customer not found",
                                Map.of("customer_id", customer_id)));

        // get all ACTIVE accounts
        List<Account> accounts = this.accountRepository
                .findAllByCustomerCustomerIdAndDeletedAtIsNullAndStatus(customer_id, AccountStatus.ACTIVE);

        // get all SUCCESSFUL transcation in the LOOK_BACK_MONTH
        List<Transaction> transactions = getAllTranscations(customer_id);

        // Without a long enough transaction history the factor calculations fall
        // back to their neutral defaults
        if (!hasEnoughHistory(customer_id)) {
            return null;
        }

        // get all factors
        Double ratio = calculateSpendingIncomeRatio(transactions);
        Double monthsCoverage = calculateMonthsCoverage(accounts, transactions);
        Double savingProgress = calculateMeanSavingProgress(customer_id);

        // calculate risk score
        boolean haveGoals = savingProgress != null;
        List<RiskScoreFactor> riskScoreFactors = new ArrayList<>();
        RiskScoreFactor factorForRatio = generateRiskScoreForFactor(RiskScoreDataElement.SPENDING_INCOME_RATIO,
                haveGoals, ratio);
        RiskScoreFactor factorForBalance = generateRiskScoreForFactor(RiskScoreDataElement.SAVING_BALANCE,
                haveGoals, monthsCoverage);
        RiskScoreFactor factorForGoal = generateRiskScoreForFactor(RiskScoreDataElement.GOAL_PROGRESS,
                haveGoals, savingProgress);

        riskScoreFactors.add(factorForRatio);
        riskScoreFactors.add(factorForBalance);
        riskScoreFactors.add(factorForGoal);

        // calculate total score
        Double riskScore = riskScoreFactors.stream().map(RiskScoreFactor::getContribution).reduce(0.0, Double::sum);

        // get correct band;
        List<RiskScoreBand> scoreBands = riskScoreRules.getRiskScoreBands();
        RiskScoreBand correctRiskScoreBand = null;
        for (RiskScoreBand b : scoreBands) {
            if (riskScore <= b.getMax()) {
                correctRiskScoreBand = b;
                break;
            }
        }

        // if risk score didn't fall into any band, it means it is over the largest band
        if (correctRiskScoreBand == null) {
            correctRiskScoreBand = scoreBands.get(scoreBands.size() - 1);
        }

        // A frozen account forces the band to HIGH regardless of other factors
        if (hasFrozenAccount(customer_id)) {
            correctRiskScoreBand = getHighBand();
            riskScoreFactors.add(buildFrozenOverrideFactor());
            log.debug("frozen account override applied for customer {}", customer_id);
        }

        RiskScore riskScoreEntity = new RiskScore();
        riskScoreEntity.setVersion(riskScoreRules.getVersion());
        riskScoreEntity.setScore(riskScore);
        riskScoreEntity.setRiskLevel(correctRiskScoreBand.getLevel());
        riskScoreEntity.setCalculateStatus(RiskScoreStatus.OK);
        try {
            riskScoreEntity.setFactors(objectMapper.writeValueAsString(riskScoreFactors));
        } catch (JacksonException e) {
            throw new IllegalStateException("Failed to serialize risk score factor", e);
        }
        riskScoreEntity.setCustomer(customer);

        return riskScoreEntity;
    }

    public RiskScoreResponse getRiskScoreById(Long customer_id, CustomUserPrincipal principal) {
        if (!this.customerRepository.existsById(customer_id)) {
            throw new NotFoundException("CUSTOMER_NOT_FOUND", "Customer not found",
                    Map.of("customer_id", customer_id));
        }

        RiskScore recentRiskScore = this.riskScoreRepository
                .findFirstByCustomerCustomerIdOrderByCalculatedAtDesc(customer_id)
                .orElseThrow(() -> new NotFoundException("RISK_SCORE_NOT_FOUND", "Customer doesn't have risk score yet",
                        Map.of("customer_id", customer_id)));

        return riskScoreMapper.toResponse(recentRiskScore);
    }

    public List<RiskScoreResponse> getRiskScoreHistory(Long customer_id) {
        if (!this.customerRepository.existsById(customer_id)) {
            throw new NotFoundException("CUSTOMER_NOT_FOUND", "Customer not found",
                    Map.of("customer_id", customer_id));
        }

        List<RiskScore> scoreHistory = this.riskScoreRepository
                .findAllByCustomerCustomerIdOrderByCalculatedAtDesc(customer_id)
                .orElseThrow(() -> new NotFoundException("RISK_SCORE_NOT_FOUND", "Customer doesn't have risk score yet",
                        Map.of("customer_id", customer_id)));

        return scoreHistory.stream().map(record -> riskScoreMapper.toResponse(record)).toList();
    }

    // check if the customer's transcation history is long enough
    // if not, the risk assessment is not meaningful
    private boolean hasEnoughHistory(Long customer_id) {
        Instant earliest = this.transactionRepository.findEarliestTimestampForCustomer(customer_id);
        if (earliest == null) {
            return false;
        }

        int minMonths = riskScoreRules.getInsufficientConditions().getMinMonths();
        Instant threshold = LocalDate.now(ZoneOffset.UTC).minusMonths(minMonths)
                .atStartOfDay().toInstant(ZoneOffset.UTC);

        return !earliest.isAfter(threshold);
    }

    /**
     * Scenario contract: status is INSUFFICIENT_DATA, score/level/explain are left
     * null so they are omitted from the payload, and the failure is identified by
     * a stable code rather than an ad-hoc string. Nothing is persisted — there is
     * no score to record.
     */
    private RiskScoreResponse buildInsufficientDataResponse(Long customer_id) {
        log.debug("insufficient transaction history for customer {}", customer_id);

        RiskScoreResponse response = new RiskScoreResponse();
        response.setCustomerId(customer_id);
        response.setCalculateStatus(RiskScoreStatus.INSUFFICIENT_DATA);
        response.setCode("RISK_SCORE_INSUFFICIENT_DATA");
        response.setMessage("Not enough transaction history to calculate a risk score.");
        response.setCalculatedAt(Instant.now());
        return response;
    }

    private boolean hasFrozenAccount(Long customer_id) {
        return this.accountRepository
                .existsByCustomerCustomerIdAndDeletedAtIsNullAndStatus(customer_id, AccountStatus.FROZEN);
    }

    /**
     * The HIGH band as configured in risk-score-rules.yaml. Resolved by level
     * rather than by position so reordering the bands cannot silently change
     * which band the override lands on.
     */
    private RiskScoreBand getHighBand() {
        return riskScoreRules.getRiskScoreBands().stream()
                .filter(b -> RiskScoreLevel.HIGH == b.getLevel())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "No HIGH band configured in risk score rules; cannot apply frozen account override"));
    }

    /**
     * Records the override as a triggered factor so it is visible in the response
     * and in the persisted factors JSON. Contribution is zero: the override
     * changes the band, not the weighted-sum score.
     */
    private RiskScoreFactor buildFrozenOverrideFactor() {
        RiskScoreFactor factor = new RiskScoreFactor();
        factor.setDataElement(RiskScoreDataElement.FROZEN_ACCOUNT_OVERRIDE);
        factor.setWeight(0.0);
        factor.setContribution(0.0);
        factor.setExplanation("Account frozen; risk band forced to HIGH");
        factor.setValid(true);
        return factor;
    }

    private Double calculateSpendingIncomeRatio(List<Transaction> transcations) {
        BigDecimal totalIncome = getTotalIncome(transcations);
        BigDecimal totalSpending = getTotalSpending(transcations);

        Double ratio;
        // if user has no income in the past period
        if (totalIncome.compareTo(BigDecimal.ZERO) == 0) {
            // if user also has no spending (user didn't user their any accounts)
            if (totalSpending.compareTo(BigDecimal.ZERO) == 0) {
                ratio = MIN_RATIO;
            } else {
                ratio = MAX_RATIO;
            }

        } else {
            ratio = totalSpending.divide(totalIncome, 4, RoundingMode.HALF_UP).doubleValue();
        }

        log.debug("ratio = {}", ratio);

        return ratio;

    }

    private Double calculateMonthsCoverage(List<Account> accounts, List<Transaction> transactions) {
        // get total Balance
        BigDecimal totalBalance = accounts.stream().map(Account::getBalance).reduce(BigDecimal.ZERO, BigDecimal::add);

        // calculate average spending per month for the LookUp Period
        BigDecimal totalSpending = getTotalSpending(transactions);
        BigDecimal averageSpending = totalSpending.divide(BigDecimal.valueOf(LOOK_BACK_MONTH), 4, RoundingMode.HALF_UP);

        double monthCanCover;
        if (averageSpending.compareTo(BigDecimal.ZERO) == 0) {
            monthCanCover = MAX_COVER;
        } else {
            monthCanCover = totalBalance.divide(averageSpending, 1, RoundingMode.HALF_UP).doubleValue();
        }

        log.debug("covered_month = {}", monthCanCover);

        return monthCanCover;
    }

    private Double calculateMeanSavingProgress(Long customer_id) {
        List<SavingsGoalResponse> allSavingGoalProgresses = this.savingsGoalService.getAllGoalsForCustomer(customer_id);

        Double weightedMean;
        BigDecimal weightedSum = BigDecimal.ZERO;
        BigDecimal weightSum = BigDecimal.ZERO;

        // Using weighted mean since goals may have different target amount
        for (SavingsGoalResponse goal : allSavingGoalProgresses) {
            BigDecimal targetAmount = goal.getTargetAmount();
            BigDecimal progressPercentage = goal.getProgressPercentage();

            weightedSum = weightedSum.add(targetAmount.multiply(progressPercentage));
            weightSum = weightSum.add(targetAmount);
        }

        weightedMean = weightSum.compareTo(BigDecimal.ZERO) == 0
                ? null
                : weightedSum.divide(weightSum, 2, RoundingMode.HALF_UP).doubleValue();

        log.debug("saving goal mean progress = {}", weightedMean);

        return weightedMean;
    }

    private RiskScoreFactor generateRiskScoreForFactor(RiskScoreDataElement element, boolean haveGoals, Double data) {

        RiskScoreFactor factor = new RiskScoreFactor();
        factor.setDataElement(element);

        // When saving goal is missing
        if (data == null) {
            factor.setValid(false);
            factor.setWeight(0.0);
            factor.setContribution(0.0);
            return factor;
        }

        Map<RiskScoreDataElement, FactorConfig> rules = riskScoreRules.getFactors();
        // get bands and weight for the factor
        List<Band> bands = rules.get(element).getBands();
        Double weight = rules.get(element).getWeight();

        // weight normalization if saving goal is missing
        if (!haveGoals) {
            if (element == RiskScoreDataElement.GOAL_PROGRESS) {
                weight = 0.0;
            } else if (element == RiskScoreDataElement.SPENDING_INCOME_RATIO) {
                weight = weight / (weight + rules.get(RiskScoreDataElement.SAVING_BALANCE).getWeight());
            } else if (element == RiskScoreDataElement.SAVING_BALANCE) {
                weight = weight / (weight + rules.get(RiskScoreDataElement.SPENDING_INCOME_RATIO).getWeight());
            }
        }

        Band correctBand = null;
        for (Band b : bands) {
            if (data <= b.getMax()) {
                correctBand = b;
                break;
            }
        }

        // If data doesn't fall into any band; (rare, probably won't
        // happen)
        if (correctBand == null) {
            factor.setValid(false);
            factor.setWeight(0.0);
            factor.setContribution(0.0);
            return factor;
        }

        Double contribution = weight * correctBand.getScore();
        factor.setWeight(weight);
        factor.setSubscore(correctBand.getScore());
        factor.setContribution(contribution);
        factor.setExplanation(correctBand.getExplain());
        factor.setValid(true);
        return factor;
    }

    private BigDecimal getTotalSpending(List<Transaction> transcations) {
        // Get each month spendings, incomes in the past 3 months and calculate
        // spending/income ratio
        BigDecimal totalSpending;

        List<Transaction> spendings = transcations.stream()
                .filter(t -> t.getDirection() == TransactionDirection.DEBIT
                        && t.getStatus() == TransactionStatus.SUCCESS)
                .toList();

        totalSpending = spendings.stream().map(Transaction::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        return totalSpending;
    }

    private BigDecimal getTotalIncome(List<Transaction> transcations) {
        // Get each month spendings, incomes in the past 3 months and calculate
        // spending/income ratio
        BigDecimal totalIncome;

        List<Transaction> incomes = transcations.stream()
                .filter(t -> t.getDirection() == TransactionDirection.CREDIT
                        && t.getStatus() == TransactionStatus.SUCCESS)
                .toList();

        totalIncome = incomes.stream().map(Transaction::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        return totalIncome;
    }

    private List<Transaction> getAllTranscations(Long customer_id) {
        Instant now = Instant.now();
        Instant start = LocalDate.now(ZoneOffset.UTC).minusMonths(LOOK_BACK_MONTH).atStartOfDay()
                .toInstant(ZoneOffset.UTC);

        List<Transaction> transcations = this.transactionRepository
                .findByAccount_Customer_CustomerIdAndTimestampBetweenOrderByTimestampAsc(
                        customer_id, start, now);

        return transcations;
    }

}
