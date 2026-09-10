package com.voltio.mcptestserver;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GicRateToolTest {

    private final GicRateTool gicRateTool = new GicRateTool();

    @Test
    void getGicRates_shouldReturnAllSupportedTermsWithRates() {
        String result = gicRateTool.getGicRates();

        assertThat(result)
                .contains("SIX_MONTHS: 3.00%")
                .contains("ONE_YEAR: 5.00%")
                .contains("TWO_YEARS: 5.50%")
                .contains("THREE_YEARS: 6.00%")
                .contains("FIVE_YEARS: 7.00%");
    }

    @Test
    void getGicRates_shouldReturnOneLinePerTerm() {
        String result = gicRateTool.getGicRates();

        String[] lines = result.split("\n");

        assertThat(lines).hasSize(5);
    }
}
