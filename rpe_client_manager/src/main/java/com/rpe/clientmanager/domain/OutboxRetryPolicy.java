package com.rpe.clientmanager.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * When a failed outbox event is tried again: exponential backoff ({@code initialBackoff * 2^(attempts-1)}) capped at
 * {@code maxBackoff}, giving up after {@code maxAttempts}.
 */
public record OutboxRetryPolicy(int maxAttempts, Duration initialBackoff, Duration maxBackoff) {

    public OutboxRetryPolicy {
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("maxAttempts must be at least 1");
        }
        Objects.requireNonNull(initialBackoff, "initialBackoff");
        Objects.requireNonNull(maxBackoff, "maxBackoff");
    }

    public boolean isExhausted(int attempts) {
        return attempts >= maxAttempts;
    }

    public Instant nextAttemptAt(int attempts, Instant now) {
        Duration backoff = initialBackoff.multipliedBy(1L << Math.min(Math.max(attempts - 1, 0), 30));
        return now.plus(backoff.compareTo(maxBackoff) > 0 ? maxBackoff : backoff);
    }
}
