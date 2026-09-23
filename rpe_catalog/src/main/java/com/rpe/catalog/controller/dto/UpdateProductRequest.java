package com.rpe.catalog.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Status is intentionally absent: it only changes through DELETE (cancel) and POST .../activate.
 */
public record UpdateProductRequest(
        @NotBlank @Size(max = 100) String name,
        @Size(max = 500) String description
) {
}
