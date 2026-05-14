package com.fpt.swp.journaltrendtracker.validation;

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
        // Must be in the past and year > 1920
        return dob.isBefore(now) && dob.getYear() > 1920;
    }
}
