package com.rpe.catalog.controller;

import com.rpe.catalog.controller.dto.ProductResponse;
import com.rpe.catalog.domain.Product;
import org.springframework.stereotype.Component;

/**
 * Converts domain products into the public API representation, keeping the DTOs free of domain knowledge.
 */
@Component
public class ProductMapper {

    public ProductResponse toResponse(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getStatus(),
                product.getCreatedAt(),
                product.getUpdatedAt());
    }
}
