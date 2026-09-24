package com.rpe.clientmanager.service;

import com.rpe.clientmanager.domain.OutboxEvent;

/** Delivers an outbox event to the message broker. The relay depends on this interface, not on SQS. */
public interface OutboxEventSender {

    /** Returns once the broker accepted the event. @throws RuntimeException if it didn't (the relay retries) */
    void send(OutboxEvent event);
}
