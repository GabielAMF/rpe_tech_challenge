package com.rpe.clientmanager.service;

import com.rpe.clientmanager.exception.CustomerUnderageException;
import com.rpe.clientmanager.exception.InvalidBirthDateException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.time.Period;

/**
 * Birth date rules: it must be in the past, and optionally the customer must be at least
 * {@code app.customer.minimum-age} years old.
 * <p>
 * The minimum age is disabled by default (0): it was considered, but the real value depends on the product
 * and regulation and hasn't been confirmed, so it must not be enforced until it is.
 */
@Component
public class BirthDatePolicy {

    private final Clock clock;
    private final int minimumAge;

    public BirthDatePolicy(Clock clock, @Value("${app.customer.minimum-age:0}") int minimumAge) {
        this.clock = clock;
        this.minimumAge = minimumAge;
    }

    public void validate(LocalDate birthDate) {
        LocalDate today = LocalDate.now(clock);
        if (!birthDate.isBefore(today)) {
            throw new InvalidBirthDateException();
        }
        if (minimumAge > 0 && Period.between(birthDate, today).getYears() < minimumAge) {
            throw new CustomerUnderageException(minimumAge);
        }
    }
}
