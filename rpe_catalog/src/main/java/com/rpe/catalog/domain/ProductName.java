package com.rpe.catalog.domain;

import com.rpe.catalog.exception.InvalidProductNameException;

import java.util.Locale;

/**
 * A valid product name: trimmed, upper-cased and never empty, so " gold " and "GOLD" are the same name.
 * Creating one is the only place this rule is applied.
 */
public record ProductName(String value) {

    public ProductName {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.isEmpty()) {
            throw new InvalidProductNameException();
        }
        value = trimmed.toUpperCase(Locale.ROOT);
    }

    @Override
    public String toString() {
        return value;
    }
}
