package com.rpe.clientmanager.service;

import com.rpe.clientmanager.domain.Customer;

import java.time.Instant;
import java.util.UUID;

/**
 * Event published when a customer is created, asking rpe_card_processor to produce a card. Serialized as
 * plain JSON: this record is the message contract, so renaming a field breaks the consumer.
 * <p>
 * Contains personal data (name, full CPF, credit info) because card production needs it; {@link #toString()}
 * leaves it out so the event can be logged safely.
 */
public record CardProductionRequested(
        UUID eventId,
        String eventType,
        Instant occurredAt,
        UUID customerId,
        String customerName,
        String cpf,
        String creditInfo
) {

    public static final String EVENT_TYPE = "CARD_PRODUCTION_REQUESTED";

    public static CardProductionRequested of(Customer customer, String creditInfo, Instant now) {
        return new CardProductionRequested(UUID.randomUUID(), EVENT_TYPE, now, customer.getId(),
                customer.getName(), customer.getCpf(), creditInfo);
    }

    @Override
    public String toString() {
        return "CardProductionRequested[eventId=" + eventId + ", customerId=" + customerId + "]";
    }
}
