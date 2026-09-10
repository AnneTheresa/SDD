package com.group1.banking.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.group1.banking.service.impl.SavingsChatGuardrailService.GuardrailResult;

import static org.assertj.core.api.Assertions.assertThat;

class SavingsChatGuardrailServiceTest {

    private final SavingsChatGuardrailService guardrail = new SavingsChatGuardrailService();

    @ParameterizedTest
    @ValueSource(strings = {
            "Which stock should I buy right now?",
            "Should I invest in crypto with my savings?",
            "Will I get approved for a loan increase?",
            "What's my credit score?",
            "Can I sue the bank for this fee?",
            "I have a symptom, what medication should I take?",
            "How should I file my taxes this year?"
    })
    void evaluate_blocksOutOfScopeTopics(String query) {
        GuardrailResult result = guardrail.evaluate(query);

        assertThat(result.allowed()).isFalse();
        assertThat(result.declineMessage()).isNotBlank();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "How can I save more money each month?",
            "What was my dining spend last month?",
            "How is my Travel savings goal progressing?",
            "Any tips for building an emergency fund?",
            "Why did my spending in Shopping go up?"
    })
    void evaluate_allowsInScopeQuestions(String query) {
        GuardrailResult result = guardrail.evaluate(query);

        assertThat(result.allowed()).isTrue();
        assertThat(result.declineMessage()).isNull();
    }

    @Test
    void evaluate_blocksBlankQuery() {
        GuardrailResult result = guardrail.evaluate("   ");

        assertThat(result.allowed()).isFalse();
    }

    @Test
    void evaluate_blocksNullQuery() {
        GuardrailResult result = guardrail.evaluate(null);

        assertThat(result.allowed()).isFalse();
    }
}
