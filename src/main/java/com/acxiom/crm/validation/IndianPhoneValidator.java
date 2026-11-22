package com.acxiom.crm.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class IndianPhoneValidator implements ConstraintValidator<IndianPhone, String> {

    private static final String PATTERN = "^[6-9]\\d{9}$";

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) return true; // use @NotBlank separately
        return value.matches(PATTERN);
    }
}
