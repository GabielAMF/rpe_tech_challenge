package com.rpe.clientmanager.domain;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OutboxEventTest {

    private static final Instant NOW = Instant.parse("2026-09-24T12:00:00Z");
    private static final OutboxRetryPolicy POLICY = new OutboxRetryPolicy(3, Duration.ofSeconds(2), Duration.ofMinutes(5));

    private final OutboxEvent event = new OutboxEvent(UUID.randomUUID(), "CARD_PRODUCTION_REQUESTED",
            UUID.randomUUID(), "{\"cpf\":\"12345678909\"}", NOW);

    @Test
    void startsPendingAndDueImmediately() {
        assertThat(event.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(event.getNextAttemptAt()).isEqualTo(NOW);
        assertThat(event.getAttempts()).isZero();
    }

    @Test
    void sentClearsThePayload() {
        event.markSent(NOW.plusSeconds(1));

        assertThat(event.getStatus()).isEqualTo(OutboxStatus.SENT);
        assertThat(event.getSentAt()).isEqualTo(NOW.plusSeconds(1));
        assertThat(event.getPayload()).isNull();
    }

    @Test
    void failureSchedulesARetryWithBackoff() {
        event.recordFailure("SdkClientException: connection refused", NOW, POLICY);

        assertThat(event.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(event.getAttempts()).isEqualTo(1);
        assertThat(event.getNextAttemptAt()).isEqualTo(NOW.plusSeconds(2));
        assertThat(event.getLastError()).isEqualTo("SdkClientException: connection refused");
    }

    @Test
    void failsForGoodAfterMaxAttemptsKeepingThePayloadForAResend() {
        event.recordFailure("e1", NOW, POLICY);
        event.recordFailure("e2", NOW, POLICY);
        event.recordFailure("e3", NOW, POLICY);

        assertThat(event.getStatus()).isEqualTo(OutboxStatus.FAILED);
        assertThat(event.getAttempts()).isEqualTo(3);
        assertThat(event.getPayload()).isNotNull();
        assertThatThrownBy(() -> event.markSent(NOW)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void longErrorsAreTruncated() {
        event.recordFailure("x".repeat(1000), NOW, POLICY);

        assertThat(event.getLastError()).hasSize(500);
    }

    @Test
    void sentEventCantBeSentAgain() {
        event.markSent(NOW);

        assertThatThrownBy(() -> event.markSent(NOW)).isInstanceOf(IllegalStateException.class);
    }
}
