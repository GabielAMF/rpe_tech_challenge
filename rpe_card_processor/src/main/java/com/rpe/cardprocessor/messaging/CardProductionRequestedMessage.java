package com.rpe.cardprocessor.messaging;

import java.time.Instant;
import java.util.UUID;

/**
 * The card production message published by rpe_client_manager (its {@code CardProductionRequested} record).
 * Field names are the contract. Unknown fields are ignored, so the producer can add fields safely.
 * {@code cpf} is part of the message but not needed to produce the card, so it isn't stored.
 * {@link #toString()} leaves the personal data out.
 */
public record CardProductionRequestedMessage(
        UUID eventId,
        String eventType,
        Instant occurredAt,
        UUID customerId,
        String customerName,
        String cpf,
        String creditInfo
) {

    public static final String EVENT_TYPE = "CARD_PRODUCTION_REQUESTED";

    @Override
    public String toString() {
        return "CardProductionRequestedMessage[eventId=" + eventId + ", eventType=" + eventType
                + ", customerId=" + customerId + "]";
    }
}
