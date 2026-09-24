package com.rpe.clientmanager.domain;

import com.rpe.clientmanager.repository.converter.EncryptedStringConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * An event waiting to be published (transactional outbox). The payload holds personal data: it is encrypted in the
 * database, cleared once the event is SENT, and never logged. No Lombok {@code @ToString} on purpose.
 */
@Getter
@Entity
@Table(name = "outbox_event")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OutboxEvent extends AuditableEntity {

    private static final int MAX_ERROR_LENGTH = 500;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "event_id", nullable = false, updatable = false)
    private UUID eventId;

    @Column(name = "event_type", nullable = false, updatable = false, length = 50)
    private String eventType;

    @Column(name = "aggregate_id", nullable = false, updatable = false)
    private UUID aggregateId;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "payload_encrypted")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private OutboxStatus status;

    @Column(name = "attempts", nullable = false)
    private int attempts;

    @Column(name = "next_attempt_at", nullable = false)
    private Instant nextAttemptAt;

    @Column(name = "last_error", length = MAX_ERROR_LENGTH)
    private String lastError;

    @Column(name = "sent_at")
    private Instant sentAt;

    public OutboxEvent(UUID eventId, String eventType, UUID aggregateId, String payload, Instant now) {
        this.eventId = Objects.requireNonNull(eventId, "eventId");
        this.eventType = Objects.requireNonNull(eventType, "eventType");
        this.aggregateId = Objects.requireNonNull(aggregateId, "aggregateId");
        this.payload = Objects.requireNonNull(payload, "payload");
        this.status = OutboxStatus.PENDING;
        this.nextAttemptAt = Objects.requireNonNull(now, "now");
    }

    /** Sent: the personal data in the payload is no longer needed. */
    public void markSent(Instant now) {
        requirePending();
        this.status = OutboxStatus.SENT;
        this.sentAt = now;
        this.payload = null;
        this.lastError = null;
    }

    /** Schedules the next attempt, or gives up (FAILED, payload kept for a manual resend) when out of attempts. */
    public void recordFailure(String error, Instant now, OutboxRetryPolicy retryPolicy) {
        requirePending();
        this.attempts++;
        this.lastError = error == null || error.length() <= MAX_ERROR_LENGTH ? error : error.substring(0, MAX_ERROR_LENGTH);
        if (retryPolicy.isExhausted(attempts)) {
            this.status = OutboxStatus.FAILED;
        } else {
            this.nextAttemptAt = retryPolicy.nextAttemptAt(attempts, now);
        }
    }

    private void requirePending() {
        if (status != OutboxStatus.PENDING) {
            throw new IllegalStateException("Outbox event " + id + " is " + status + ", not PENDING");
        }
    }
}
