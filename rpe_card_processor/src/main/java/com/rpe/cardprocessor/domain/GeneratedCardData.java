package com.rpe.cardprocessor.domain;

import java.time.YearMonth;

/** The generated, sensitive part of a card. {@link #toString()} hides it. */
public record GeneratedCardData(CardOperator operator, String number, YearMonth expiry, String cvv) {

    @Override
    public String toString() {
        return "GeneratedCardData[operator=" + operator + "]";
    }
}
