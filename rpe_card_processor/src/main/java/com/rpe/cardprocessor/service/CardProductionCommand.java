package com.rpe.cardprocessor.service;

import com.rpe.cardprocessor.exception.InvalidCardProductionRequestException;
import org.springframework.util.StringUtils;

import java.util.UUID;

/**
 * What the service needs to produce a card; validates itself, so an incomplete request never reaches the service.
 * {@link #toString()} hides the personal data (name, credit info).
 *
 * @throws InvalidCardProductionRequestException if eventId, customerId or holderName is missing
 */
public record CardProductionCommand(UUID eventId, UUID customerId, String holderName, String creditInfo) {

    public CardProductionCommand {
        if (eventId == null) {
            throw new InvalidCardProductionRequestException("eventId is missing");
        }
        if (customerId == null) {
            throw new InvalidCardProductionRequestException("customerId is missing");
        }
        if (!StringUtils.hasText(holderName)) {
            throw new InvalidCardProductionRequestException("customerName is missing");
        }
    }

    @Override
    public String toString() {
        return "CardProductionCommand[eventId=" + eventId + ", customerId=" + customerId + "]";
    }
}
