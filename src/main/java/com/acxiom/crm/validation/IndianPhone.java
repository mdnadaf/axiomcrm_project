package com.acxiom.crm.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = IndianPhoneValidator.class)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface IndianPhone {
    String message() default "Enter a valid phone number.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
