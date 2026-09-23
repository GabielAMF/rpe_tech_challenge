package com.rpe.clientmanager.client.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Response of rpe_card_processor's {@code GET /api/v1/customers/{customerId}/card}. Provisional contract
 * (stubbed by WireMock) until rpe_card_processor is implemented.
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
