package com.rpe.catalog.controller.dto;

import com.rpe.catalog.domain.ProductStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * Public representation of a product. rpe_card_processor reads this shape through its Feign client,
 * so field changes here are breaking changes for that service. Built by ProductMapper.
 */
public record ProductResponse(
        UUID id,
        String name,
        String description,
        ProductStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}
