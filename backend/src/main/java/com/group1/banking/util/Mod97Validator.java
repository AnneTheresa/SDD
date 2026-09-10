package com.group1.banking.util;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Modulo 97 IBAN checksum validator. (T009)
 * Pure algorithm — no external dependencies.
 * Validates account strings of 5–34 characters using the ISO 7064 MOD-97-10 algorithm.
 */
public class Mod97Validator implements ConstraintValidator<ValidPayeeAccount, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return false;
        }
        return isValidMod97(value);
    }

    /**
     * Validates a string using the Modulo 97 algorithm.
     * Moves first 4 chars to end, converts letters to digits (A=10, B=11, ...), computes MOD 97.
     * Valid if remainder == 1.
     */
    public static boolean isValidMod97(String account) {
        if (account == null) return false;
        String cleaned = account.replaceAll("\\s", "");
        if (cleaned.length() < 5 || cleaned.length() > 34) {
            return false;
        }
        // Rearrange: move first 4 characters to the end
        String rearranged = cleaned.substring(4) + cleaned.substring(0, 4);
        // Convert each character to numeric
        StringBuilder numeric = new StringBuilder();
        for (char c : rearranged.toCharArray()) {
            if (Character.isLetter(c)) {
                numeric.append(Character.toUpperCase(c) - 'A' + 10);
            } else if (Character.isDigit(c)) {
                numeric.append(c);
            } else {
                return false;
            }
        }
        // Compute MOD 97 using chunked big-number arithmetic
        String numStr = numeric.toString();
        int remainder = 0;
        for (int i = 0; i < numStr.length(); i++) {
            remainder = (remainder * 10 + (numStr.charAt(i) - '0')) % 97;
        }
        return remainder == 1;
    }
}
