package com.group1.banking.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import com.group1.banking.DigitalBankingPlatformApplication;
import com.group1.banking.config.RiskScoreRules.Band;
import com.group1.banking.config.RiskScoreRules.FactorConfig;
import com.group1.banking.enums.RiskScoreDataElement;

/**
 * Verifies risk-score-rules.yaml actually binds into RiskScoreRules.
 * Binding failures are silent (empty collections, not errors), so this
 * guards every later step that reads the rules.
 */
@SpringBootTest(classes = DigitalBankingPlatformApplication.class)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:rulesbindtest",
        "spring.datasource.driverClassName=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class RiskScoreRulesTest {

    @Autowired
    private RiskScoreRules rules;

    @Test
    void bindsTopLevelValues() {
        assertThat(rules.getVersion()).isEqualTo(1.0);
        assertThat(rules.getInsufficientConditions()).isNotNull();
        assertThat(rules.getInsufficientConditions().getMinMonths()).isEqualTo(3);
    }

    @Test
    void bindsAllThreeFactorKeys() {
        assertThat(rules.getFactors()).containsOnlyKeys(
                RiskScoreDataElement.SPENDING_INCOME_RATIO,
                RiskScoreDataElement.SAVING_BALANCE,
                RiskScoreDataElement.GOAL_PROGRESS);
    }

    @Test
    void bindsWeightsSummingToOne() {
        double total = rules.getFactors().values().stream()
                .mapToDouble(FactorConfig::getWeight)
                .sum();
        assertThat(total).isEqualTo(1.0, org.assertj.core.data.Offset.offset(1e-9));
    }

    @Test
    void bindsBandsWithAllFields() {
        assertThat(rules.getFactors().get(RiskScoreDataElement.SPENDING_INCOME_RATIO)
                .getBands()).hasSize(5);
        assertThat(rules.getFactors().get(RiskScoreDataElement.SAVING_BALANCE)
                .getBands()).hasSize(5);
        assertThat(rules.getFactors().get(RiskScoreDataElement.GOAL_PROGRESS)
                .getBands()).hasSize(6);

        Band first = rules.getFactors().get(RiskScoreDataElement.SPENDING_INCOME_RATIO)
                .getBands().get(0);
        assertThat(first.getMax()).isEqualTo(0.5);
        assertThat(first.getScore()).isEqualTo(10);
        assertThat(first.getExplain()).isEqualTo("healthy");
    }

    /**
     * Band matching uses findFirst over the list, so ascending max order
     * is a correctness requirement, not a style preference.
     */
    @Test
    void bandsAreInAscendingMaxOrder() {
        rules.getFactors().forEach((element, factor) -> {
            List<Band> bands = factor.getBands();
            for (int i = 1; i < bands.size(); i++) {
                assertThat(bands.get(i).getMax())
                        .as("%s band %d must exceed previous", element, i)
                        .isGreaterThan(bands.get(i - 1).getMax());
            }
        });
    }
}
