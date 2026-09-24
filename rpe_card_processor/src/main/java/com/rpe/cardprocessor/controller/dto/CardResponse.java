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

    /**
     * The product as rpe_catalog describes it now; if the catalog couldn't be read, the card's issue-time snapshot
     * with a null {@code status}.
     */
    public record ProductResponse(UUID id, String name, String description, String status) {
    }
}
