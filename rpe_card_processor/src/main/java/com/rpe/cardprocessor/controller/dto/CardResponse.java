package com.rpe.cardprocessor.controller.dto;

import com.rpe.cardprocessor.domain.CardStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * A customer's card as rpe_client_manager consumes it (its {@code CardProcessorCardResponse} mirrors this record).
 * Only the masked number leaves the service: never the full number, expiry, CVV or holder name.
 */
public record CardResponse(
        UUID cardId,
        CardStatus status,
        String maskedNumber,
        ProductResponse product,
        Instant createdAt
) {

    /** {@code status} is the product's current status in rpe_catalog, null if the catalog couldn't be read. */
    public record ProductResponse(UUID id, String name, String description, String status) {
    }
}
