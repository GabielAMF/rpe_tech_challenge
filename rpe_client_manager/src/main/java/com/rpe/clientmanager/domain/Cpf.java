package com.rpe.clientmanager.domain;

import com.rpe.clientmanager.exception.InvalidCpfException;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * A CPF, stored as 11 upper-case letters or digits with formatting removed ("123.456.789-09" → "12345678909").
 * Letters are accepted because the CPF is expected to become alphanumeric; check digits are not validated
 * until the alphanumeric rules are defined.
 * <p>
 * {@link #toString()} is masked so a CPF can't leak into logs by accident; use {@link #value()} explicitly.
 */
public record Cpf(String value) {

    private static final Pattern FORMATTING = Pattern.compile("[.\\-\\s]");
    private static final Pattern VALID = Pattern.compile("[0-9A-Z]{11}");

    public Cpf {
        String normalized = value == null ? "" : FORMATTING.matcher(value).replaceAll("").toUpperCase(Locale.ROOT);
        if (!VALID.matcher(normalized).matches()) {
            throw new InvalidCpfException();
        }
        value = normalized;
    }

    /** Only the last two characters are visible: {@code ***.***.***-09}. */
    public String masked() {
        return "***.***.***-" + value.substring(9);
    }

    @Override
    public String toString() {
        return masked();
    }
}
