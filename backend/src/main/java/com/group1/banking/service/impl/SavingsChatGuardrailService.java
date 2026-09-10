package com.group1.banking.service.impl;

import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;

/**
 * Fast, pre-LLM guardrail for the Savings Insight Chatbot. Blocks out-of-scope
 * or
 * regulated-advice topics (investment recommendations, loan approval decisions,
 * legal
 * advice, medical advice) before spending a model call, in addition to the
 * system
 * prompt's own scope instructions (defense in depth).
 *
 * This is intentionally a simple keyword/phrase filter rather than a second
 * model
 * call, so the bounded set of prompt/response interactions stays deterministic
 * enough
 * for QA to validate.
 */
@Service
public class SavingsChatGuardrailService {

    /**
     * Phrases that indicate a request for advice outside the approved scope.
     * Matched
     * as substrings against the lower-cased query, so keep entries reasonably
     * specific
     * to avoid false positives on legitimate savings questions.
     */
    private static final List<String> BLOCKED_TOPIC_PHRASES = List.of(
            // Investment advice
            "which stock", "what stock", "buy stock", "sell stock", "invest in", "investment advice",
            "should i invest", "crypto", "cryptocurrency", "bitcoin", "etf", "mutual fund",
            "portfolio allocation", "day trading",
            // Loan / credit decisions
            "loan approval", "will i get approved", "approve my loan", "increase my credit limit",
            "credit score", "mortgage approval", "am i approved",
            // Legal advice
            "legal advice", "sue the bank", "is it legal", "lawsuit", "lawyer",
            // Medical advice
            "medical advice", "diagnos", "symptom", "medication", "prescription",
            // Tax advice
            "tax advice", "file my taxes", "tax deduction", "tax return",
            // Risk Assessment Detail
            "my risk score", "my risk band", "my risk level", "my risk rating",
            "am i high risk", "how risky am i", "risk assessment", "risk factors",
            "credit rating");

    private static final String DECLINE_MESSAGE = "I can help with savings, budgeting, and general spending questions about your accounts, "
            + "but I'm not able to advise on that topic. For investment, loan, legal, medical, or tax "
            + "matters, please speak with a licensed advisor or contact our support team.";

    public GuardrailResult evaluate(String rawQuery) {
        if (rawQuery == null || rawQuery.isBlank()) {
            return GuardrailResult.blocked(
                    "I didn't receive a question - could you try asking again?");
        }

        String normalized = rawQuery.toLowerCase(Locale.ROOT);
        for (String phrase : BLOCKED_TOPIC_PHRASES) {
            if (normalized.contains(phrase)) {
                return GuardrailResult.blocked(DECLINE_MESSAGE);
            }
        }

        return GuardrailResult.allowedResult();
    }

    public record GuardrailResult(boolean allowed, String declineMessage) {
        public static GuardrailResult allowedResult() {
            return new GuardrailResult(true, null);
        }

        public static GuardrailResult blocked(String message) {
            return new GuardrailResult(false, message);
        }
    }
}
