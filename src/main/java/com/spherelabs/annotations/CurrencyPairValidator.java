package com.spherelabs.annotations;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Optional;
import java.util.regex.Pattern;

public class CurrencyPairValidator implements ConstraintValidator<CurrencyPair, String> {
    private static final Pattern pattern = Pattern.compile("^[A-Z]{3}/[A-Z]{3}$");

    @Override
    public boolean isValid(String currencyPair, ConstraintValidatorContext context) {
        return Optional.ofNullable(currencyPair)
                .map(s -> pattern.matcher(s).matches())
                // we already have the @NotNull annotation on the field, so we can return true here safely
                .orElse(true);
    }
}
