package com.rpe.cardprocessor.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Card networks. The operator is assigned at random when a card is produced and its number is generated with the
 * operator's prefix (IIN), so number and operator always agree. Stored as its name.
 */
@Getter
@RequiredArgsConstructor
public enum CardOperator {

    VISA("4"),
    MASTERCARD("51"),
    ELO("636368");

    private final String numberPrefix;
}
