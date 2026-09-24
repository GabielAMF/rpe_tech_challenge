package com.rpe.cardprocessor.service;

import java.util.UUID;

/**
 * What the service needs to produce a card. {@link #toString()} hides the personal data (name, credit info).
 */
public record CardProductionCommand(UUID eventId, UUID customerId, String holderName, String creditInfo) {

    @Override
    public String toString() {
        return "CardProductionCommand[eventId=" + eventId + ", customerId=" + customerId + "]";
    }
}
