package com.rpe.cardprocessor.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.util.UUID;

/**
 * Snapshot of the rpe_catalog product the card was issued for, as it was at issue time. Stored with the card so
 * reading a card doesn't depend on the catalog.
 */
@Embeddable
public record CardProduct(
        @Column(name = "product_id", nullable = false) UUID id,
        @Column(name = "product_name", nullable = false, length = 100) String name,
        @Column(name = "product_description", length = 500) String description
) {
}
