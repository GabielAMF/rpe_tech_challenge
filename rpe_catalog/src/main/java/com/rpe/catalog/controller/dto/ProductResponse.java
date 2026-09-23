package com.rpe.catalog.controller.dto;

import com.rpe.catalog.domain.Product;
import com.rpe.catalog.domain.ProductStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * Public representation of a product. rpe_card_processor reads this shape through its Feign client,
 * so field changes here are breaking changes for that service.
 */
public record ProductResponse(
        UUID id,
        String name,
        String description,
        ProductStatus status,
        Instant createdAt,
        Instant updatedAt
) {

    public static ProductResponse from(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getStatus(),
                product.getCreatedAt(),
                product.getUpdatedAt());
    }
}
