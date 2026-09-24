package com.rpe.clientmanager.service;

import com.rpe.clientmanager.config.OutboxProperties;
import com.rpe.clientmanager.domain.OutboxEvent;
import com.rpe.clientmanager.domain.OutboxRetryPolicy;
import com.rpe.clientmanager.domain.OutboxStatus;
import com.rpe.clientmanager.exception.EventPublishingException;
import com.rpe.clientmanager.repository.OutboxEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionOperations;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxRelayTest {

    private static final Instant NOW = Instant.parse("2026-09-24T12:00:00Z");

    @Mock
    private OutboxEventRepository repository;

    @Mock
    private OutboxEventSender sender;

    private OutboxRelay relay;

    @BeforeEach
    void setUp() {
        OutboxProperties properties = new OutboxProperties(50, 3, Duration.ofSeconds(2), Duration.ofMinutes(5),
                Duration.ofDays(7), "unused");
        relay = new OutboxRelay(repository, sender,
                new OutboxRetryPolicy(3, Duration.ofSeconds(2), Duration.ofMinutes(5)), properties,
                TransactionOperations.withoutTransaction(), Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void sendsDueEventsAndMarksThemSent() {
        OutboxEvent first = event();
        OutboxEvent second = event();
        when(repository.lockDue(NOW, 50)).thenReturn(List.of(first, second));

        assertThat(relay.relayBatch()).isEqualTo(2);

        verify(sender).send(first);
        verify(sender).send(second);
        assertThat(first.getStatus()).isEqualTo(OutboxStatus.SENT);
        assertThat(second.getStatus()).isEqualTo(OutboxStatus.SENT);
        assertThat(first.getPayload()).isNull();
    }

    @Test
    void failureSchedulesRetryAndStopsTheBatch() {
        OutboxEvent first = event();
        OutboxEvent second = event();
        when(repository.lockDue(NOW, 50)).thenReturn(List.of(first, second));
        doThrow(new EventPublishingException(first.getEventId(), "SQS did not accept it",
                new IllegalStateException("connection refused"))).when(sender).send(first);

        assertThat(relay.relayBatch()).isZero();

        assertThat(first.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(first.getAttempts()).isEqualTo(1);
        assertThat(first.getNextAttemptAt()).isEqualTo(NOW.plusSeconds(2));
        assertThat(first.getLastError()).isEqualTo("IllegalStateException: connection refused");
        verify(sender, never()).send(second);
        assertThat(second.getAttempts()).isZero();
    }

    @Test
    void scheduledRunUsesTheTransaction() {
        when(repository.lockDue(NOW, 50)).thenReturn(List.of());

        relay.relayPending();

        verify(repository).lockDue(NOW, 50);
    }

    private static OutboxEvent event() {
        return new OutboxEvent(UUID.randomUUID(), CardProductionRequested.EVENT_TYPE, UUID.randomUUID(), "{}", NOW);
    }
}
