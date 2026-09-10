package com.voltio.mcptestserver;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

/**
 * Exposes mocked Guaranteed Investment Certificate (GIC) term rates over MCP.
 * Values here are static/mocked and intentionally have no shared dependency
 * on the backend's {@code GicTerm} enum - this module stays independently
 * deployable from the banking-platform backend it serves.
 */
@Component
public class GicRateTool {

    @Tool(description = "Returns current annual GIC (Guaranteed Investment Certificate) rates "
            + "for all supported terms (SIX_MONTHS, ONE_YEAR, TWO_YEARS, THREE_YEARS, FIVE_YEARS). "
            + "Use alongside GIC educational knowledge base content to give a complete answer to "
            + "rate and suitability questions.")
    public String getGicRates() {
        return "SIX_MONTHS: 3.00%\n"
                + "ONE_YEAR: 5.00%\n"
                + "TWO_YEARS: 5.50%\n"
                + "THREE_YEARS: 6.00%\n"
                + "FIVE_YEARS: 7.00%";
    }
}
