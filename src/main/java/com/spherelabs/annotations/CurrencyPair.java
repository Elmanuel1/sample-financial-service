package com.spherelabs.annotations;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Constraint(validatedBy = CurrencyPairValidator.class)
@Target({ElementType.RECORD_COMPONENT, ElementType.TYPE, ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface CurrencyPair {
    String message() default "Currency pair is invalid";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
