package com.group1.banking.mapper;

import java.util.List;

import org.springframework.stereotype.Component;

import com.group1.banking.config.RiskScoreRules;
import com.group1.banking.config.RiskScoreRules.RiskScoreBand;
import com.group1.banking.dto.RiskScoreFactor;
import com.group1.banking.dto.RiskScoreResponse;
import com.group1.banking.entity.RiskScore;

import lombok.RequiredArgsConstructor;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/**
 * Rebuilds the response payload from a persisted score. The entity stores the
 * band but not its prose, so the explanation is resolved from the current rules
 * rather than from the row; factors are held as JSON and parsed back here.
 */
@Component
@RequiredArgsConstructor
public class RiskScoreMapper {
    private final RiskScoreRules riskScoreRules;
    private final JsonMapper objectMapper;

    public RiskScoreResponse toResponse(RiskScore riskScore) {
        String explain = riskScoreRules.getRiskScoreBands().stream()
                .filter(b -> b.getLevel() == riskScore.getRiskLevel())
                .map(RiskScoreBand::getOverallExplain).findFirst().orElse(null);

        return RiskScoreResponse.builder()
                .customerId(riskScore.getCustomer().getCustomerId())
                .riskScoreId(riskScore.getId()).score(riskScore.getScore())
                .level(riskScore.getRiskLevel()).overAllExplain(explain)
                .factors(parseFactors(riskScore.getFactors()))
                .calculateStatus(riskScore.getCalculateStatus()).calculatedAt(riskScore.getCalculatedAt())
                .build();
    }

    private List<RiskScoreFactor> parseFactors(String factorsJson) {
        if (factorsJson == null || factorsJson.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(factorsJson, new TypeReference<List<RiskScoreFactor>>() {
            });
        } catch (JacksonException e) {
            throw new IllegalStateException("Failed to deserialize risk score factors", e);
        }
    }

}
