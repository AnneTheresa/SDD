package com.group1.banking.util;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

/**
 * Custom bean validation annotation that triggers Modulo 97 IBAN checksum validation.
 */
@Documented
@Constraint(validatedBy = Mod97Validator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidPayeeAccount {
    String message() default "Invalid payee account — Modulo 97 checksum failure";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
