package com.fpt.swp.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.time.LocalDate;

public class DobValidator implements ConstraintValidator<ValidDob, LocalDate> {

    @Override
    public boolean isValid(LocalDate dob, ConstraintValidatorContext context) {
        if (dob == null) {
            return true; // Use @NotNull for null checks
        }
        
        LocalDate now = LocalDate.now();
        // Must be at least 18 years old and year > 1920
        return dob.getYear() >= 1920 && !dob.isAfter(now.minusYears(18));
    }
}
