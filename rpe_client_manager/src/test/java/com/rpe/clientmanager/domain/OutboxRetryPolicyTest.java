package com.rpe.clientmanager.domain;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OutboxRetryPolicyTest {

    private static final Instant NOW = Instant.parse("2026-09-24T12:00:00Z");
    private final OutboxRetryPolicy policy = new OutboxRetryPolicy(12, Duration.ofSeconds(2), Duration.ofMinutes(5));

    @Test
    void backoffDoublesUpToTheCap() {
        assertThat(policy.nextAttemptAt(1, NOW)).isEqualTo(NOW.plusSeconds(2));
        assertThat(policy.nextAttemptAt(2, NOW)).isEqualTo(NOW.plusSeconds(4));
        assertThat(policy.nextAttemptAt(8, NOW)).isEqualTo(NOW.plusSeconds(256));
        assertThat(policy.nextAttemptAt(9, NOW)).isEqualTo(NOW.plus(Duration.ofMinutes(5)));
        assertThat(policy.nextAttemptAt(100, NOW)).isEqualTo(NOW.plus(Duration.ofMinutes(5)));
    }

    @Test
    void exhaustedAtMaxAttempts() {
        assertThat(policy.isExhausted(11)).isFalse();
        assertThat(policy.isExhausted(12)).isTrue();
    }

    @Test
    void needsAtLeastOneAttempt() {
        assertThatThrownBy(() -> new OutboxRetryPolicy(0, Duration.ofSeconds(1), Duration.ofSeconds(1)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
