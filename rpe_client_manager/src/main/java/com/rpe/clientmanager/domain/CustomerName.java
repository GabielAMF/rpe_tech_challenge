package com.rpe.clientmanager.domain;

import com.rpe.clientmanager.exception.InvalidCustomerNameException;

/**
 * A customer's name, trimmed and never empty. Personal data: {@link #toString()} hides it so it can't leak into logs
 * by accident; use {@link #value()} explicitly.
 */
public record CustomerName(String value) {

    public CustomerName {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.isEmpty()) {
            throw new InvalidCustomerNameException();
        }
        value = trimmed;
    }

    @Override
    public String toString() {
        return "CustomerName[***]";
    }
}
