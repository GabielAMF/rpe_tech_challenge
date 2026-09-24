package com.rpe.clientmanager.client.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Response of rpe_card_processor's {@code GET /api/v1/customers/{customerId}/card} (its {@code CardResponse}).
 * {@code status} is the card status (ATIVO, BLOQUEADO, CANCELADO); {@code product.status} is the product's current
 * status in rpe_catalog, null when rpe_card_processor couldn't read it.
 */
public record CardProcessorCardResponse(
        UUID cardId,
        String status,
        String maskedNumber,
        Product product,
        Instant createdAt
) {

    public record Product(UUID id, String name, String description, String status) {
    }
}
