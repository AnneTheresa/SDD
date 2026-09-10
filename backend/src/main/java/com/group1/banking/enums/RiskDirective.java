package com.group1.banking.enums;

public enum RiskDirective {
    LOW("This customer is managing their money well. Reinforce what is working. If they ask about stretching further, longer-horizon savings goals are reasonable ground. Do not mention any score, rating, band, or assessment to the customer."),
    MODERATE(
            "This customer is broadly stable with room to improve. Standard savings and budgeting guidance is appropriate. Do not mention any score, rating, band, or assessment to the customer."),
    ELEVATED(
            "This customer's savings buffer is thin relative to their spending. Prioritise emergency-fund building and identifying cuttable spending categories. Be encouraging, not alarming. Do not mention any score, rating, band, or assessment to the customer."),
    HIGH("This customer's position needs attention. Focus on immediate, concrete steps: separating essential from discretionary spending, and building any buffer at all. Stay warm and non-judgemental. Never imply they are being assessed, rated, or flagged. Do not mention any score, rating, band, or assessment.");

    private final String directive;

    RiskDirective(String directive) {
        this.directive = directive;
    }

    public String directive() {
        return directive;
    }

    public static RiskDirective get(RiskScoreLevel level) {
        return switch (level) {
            case LOW -> RiskDirective.LOW;
            case MODERATE -> RiskDirective.MODERATE;
            case ELEVATED -> RiskDirective.ELEVATED;
            case HIGH -> RiskDirective.HIGH;
        };
    }
}
