package com.group1.banking.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for Mod97Validator.
 */
class Mod97ValidatorTest {

    private final Mod97Validator validator = new Mod97Validator();

    // ===== isValid (constraint validator) TESTS =====

    @Test
    void validIban_returnsTrue() {
        // GB82WEST12345698765432 is a standard test IBAN
        assertThat(validator.isValid("GB82WEST12345698765432", null)).isTrue();
    }

    @Test
    void invalidIban_returnsFalse() {
        assertThat(validator.isValid("GB00WEST12345698765432", null)).isFalse();
    }

    @Test
    void null_returnsFalse() {
        assertThat(validator.isValid(null, null)).isFalse();
    }

    @Test
    void blankString_returnsFalse() {
        assertThat(validator.isValid("  ", null)).isFalse();
    }

    @Test
    void emptyString_returnsFalse() {
        assertThat(validator.isValid("", null)).isFalse();
    }

    @Test
    void tooShort_returnsFalse() {
        assertThat(validator.isValid("GB82", null)).isFalse();
    }

    @Test
    void tooLong_returnsFalse() {
        String longIban = "G" + "1".repeat(34);
        assertThat(validator.isValid(longIban, null)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "DE89370400440532013000",
        "FR7630006000011234567890189"
    })
    void validEuropeanIbans_returnTrue(String iban) {
        assertThat(validator.isValid(iban, null)).isTrue();
    }

    // ===== isValidMod97 (static) TESTS =====

    @Test
    void staticMethod_validIban_returnsTrue() {
        assertThat(Mod97Validator.isValidMod97("GB82WEST12345698765432")).isTrue();
    }

    @Test
    void staticMethod_invalidIban_returnsFalse() {
        assertThat(Mod97Validator.isValidMod97("GB00WEST12345698765432")).isFalse();
    }

    @Test
    void staticMethod_null_returnsFalse() {
        assertThat(Mod97Validator.isValidMod97(null)).isFalse();
    }

    @Test
    void staticMethod_tooShort_returnsFalse() {
        assertThat(Mod97Validator.isValidMod97("AB12")).isFalse();
    }

    @Test
    void staticMethod_tooLong35chars_returnsFalse() {
        String tooLong = "A".repeat(35);
        assertThat(Mod97Validator.isValidMod97(tooLong)).isFalse();
    }

    @Test
    void staticMethod_exactly5chars_processesCorrectly() {
        // 5 chars is minimum. "GB82W" — just verify it doesn't crash
        // GB82W is invalid but should return false not throw
        assertThat(Mod97Validator.isValidMod97("GB82W")).isFalse();
    }

    @Test
    void staticMethod_specialChars_returnsFalse() {
        assertThat(Mod97Validator.isValidMod97("GB82WEST1234!@#$%")).isFalse();
    }

    @Test
    void staticMethod_withSpaces_stripsAndProcesses() {
        // GB82 WEST 1234 5698 7654 32 — same as GB82WEST12345698765432 but with spaces
        assertThat(Mod97Validator.isValidMod97("GB82 WEST 1234 5698 7654 32")).isTrue();
    }

    @Test
    void staticMethod_deIban_valid() {
        assertThat(Mod97Validator.isValidMod97("DE89370400440532013000")).isTrue();
    }
}
