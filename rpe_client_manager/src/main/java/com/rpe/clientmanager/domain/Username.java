package com.rpe.clientmanager.domain;

import com.rpe.clientmanager.exception.InvalidUsernameException;

import java.util.Locale;

/**
 * A valid username: trimmed, lower-cased and never empty, so " Admin " and "admin" are the same user.
 */
public record Username(String value) {

    public Username {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.isEmpty()) {
            throw new InvalidUsernameException();
        }
        value = trimmed.toLowerCase(Locale.ROOT);
    }

    @Override
    public String toString() {
        return value;
    }
}
