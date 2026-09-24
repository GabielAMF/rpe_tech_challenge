package com.rpe.cardprocessor.client.dto;

import java.time.Instant;
import java.util.UUID;

/** rpe_catalog's ProductResponse ({@code GET /api/v1/products/{id}}). */
public record CatalogProductResponse(
        UUID id,
        String name,
        String description,
        String status,
        Instant createdAt,
        Instant updatedAt
) {
}
