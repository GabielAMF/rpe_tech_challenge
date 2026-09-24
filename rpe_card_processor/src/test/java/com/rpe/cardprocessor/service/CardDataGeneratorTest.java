package com.rpe.cardprocessor.service;

import com.rpe.cardprocessor.TestCardProperties;
import com.rpe.cardprocessor.domain.CardOperator;
import com.rpe.cardprocessor.domain.GeneratedCardData;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.EnumSet;
import java.util.Random;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class CardDataGeneratorTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-09-23T12:00:00Z"), ZoneOffset.UTC);

    private final CardDataGenerator generator =
            new CardDataGenerator(new Random(42), CLOCK, TestCardProperties.properties());

    @Test
    void luhnCheckDigitMatchesKnownExample() {
        // Wikipedia's Luhn example: 7992739871 + check digit 3.
        assertThat(CardDataGenerator.luhnCheckDigit("7992739871")).isEqualTo(3);
    }

    @RepeatedTest(50)
    void numberHasOperatorPrefixSixteenDigitsAndValidLuhn() {
        GeneratedCardData card = generator.generate();

        assertThat(card.number()).hasSize(16).containsOnlyDigits().startsWith(card.operator().getNumberPrefix());
        assertThat(isLuhnValid(card.number())).isTrue();
        assertThat(card.cvv()).hasSize(3).containsOnlyDigits();
        assertThat(card.expiry()).isEqualTo(YearMonth.of(2031, 9));
    }

    @Test
    void operatorIsRandomAcrossAllOperators() {
        Set<CardOperator> seen = EnumSet.noneOf(CardOperator.class);
        for (int i = 0; i < 100; i++) {
            seen.add(generator.generate().operator());
        }
        assertThat(seen).containsExactlyInAnyOrder(CardOperator.values());
    }

    @Test
    void toStringHidesSensitiveData() {
        GeneratedCardData card = generator.generate();

        assertThat(card.toString()).doesNotContain(card.number(), card.cvv(), card.expiry().toString());
    }

    private static boolean isLuhnValid(String number) {
        return CardDataGenerator.luhnCheckDigit(number.substring(0, number.length() - 1))
                == number.charAt(number.length() - 1) - '0';
    }
}
