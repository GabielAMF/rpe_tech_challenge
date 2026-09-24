package com.rpe.cardprocessor.service;

import com.rpe.cardprocessor.config.CardProperties;
import com.rpe.cardprocessor.domain.CardOperator;
import com.rpe.cardprocessor.domain.GeneratedCardData;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.YearMonth;
import java.util.random.RandomGenerator;

/**
 * Generates a card's operator, number, expiry and CVV.
 * <ul>
 *   <li>operator: random (placeholder until a real assignment rule exists);</li>
 *   <li>number: 16 digits = operator prefix + random digits + Luhn check digit;</li>
 *   <li>expiry: current month plus {@code app.card.validity-years};</li>
 *   <li>CVV: 3 random digits.</li>
 * </ul>
 * Numbers are random, not guaranteed unique; fine for simulated cards.
 */
@Component
public class CardDataGenerator {

    static final int NUMBER_LENGTH = 16;

    private final RandomGenerator random;
    private final Clock clock;
    private final int validityYears;

    public CardDataGenerator(RandomGenerator random, Clock clock, CardProperties properties) {
        this.random = random;
        this.clock = clock;
        this.validityYears = properties.validityYears();
    }

    public GeneratedCardData generate() {
        CardOperator[] operators = CardOperator.values();
        CardOperator operator = operators[random.nextInt(operators.length)];
        String number = generateNumber(operator.getNumberPrefix());
        YearMonth expiry = YearMonth.now(clock).plusYears(validityYears);
        String cvv = String.format("%03d", random.nextInt(1000));
        return new GeneratedCardData(operator, number, expiry, cvv);
    }

    private String generateNumber(String prefix) {
        StringBuilder digits = new StringBuilder(prefix);
        while (digits.length() < NUMBER_LENGTH - 1) {
            digits.append(random.nextInt(10));
        }
        return digits.append(luhnCheckDigit(digits)).toString();
    }

    /** The digit that makes the whole number pass the Luhn checksum (used by every card network). */
    static int luhnCheckDigit(CharSequence digitsWithoutCheck) {
        int sum = 0;
        boolean doubleIt = true; // the rightmost digit before the check digit is doubled
        for (int i = digitsWithoutCheck.length() - 1; i >= 0; i--) {
            int digit = digitsWithoutCheck.charAt(i) - '0';
            if (doubleIt) {
                digit *= 2;
                if (digit > 9) {
                    digit -= 9;
                }
            }
            sum += digit;
            doubleIt = !doubleIt;
        }
        return (10 - sum % 10) % 10;
    }
}
