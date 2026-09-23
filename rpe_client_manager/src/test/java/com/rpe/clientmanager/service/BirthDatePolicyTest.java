package com.rpe.clientmanager.service;

import com.rpe.clientmanager.exception.CustomerUnderageException;
import com.rpe.clientmanager.exception.InvalidBirthDateException;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BirthDatePolicyTest {

    // "Today" is 2026-09-23.
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-09-23T12:00:00Z"), ZoneOffset.UTC);

    @Test
    void acceptsPastDate() {
        assertThatCode(() -> new BirthDatePolicy(CLOCK, 0).validate(LocalDate.of(1990, 5, 20)))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsTodayAndFutureDates() {
        BirthDatePolicy policy = new BirthDatePolicy(CLOCK, 0);

        assertThatThrownBy(() -> policy.validate(LocalDate.of(2026, 9, 23))).isInstanceOf(InvalidBirthDateException.class);
        assertThatThrownBy(() -> policy.validate(LocalDate.of(2030, 1, 1))).isInstanceOf(InvalidBirthDateException.class);
    }

    @Test
    void minimumAgeIsNotEnforcedByDefault() {
        assertThatCode(() -> new BirthDatePolicy(CLOCK, 0).validate(LocalDate.of(2020, 1, 1)))
                .doesNotThrowAnyException();
    }

    @Test
    void minimumAgeIsEnforcedWhenConfigured() {
        BirthDatePolicy policy = new BirthDatePolicy(CLOCK, 18);

        assertThatThrownBy(() -> policy.validate(LocalDate.of(2008, 9, 24))).isInstanceOf(CustomerUnderageException.class);
        assertThatCode(() -> policy.validate(LocalDate.of(2008, 9, 23))).doesNotThrowAnyException();
    }
}
