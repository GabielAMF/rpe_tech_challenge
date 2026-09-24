package com.rpe.cardprocessor.service;

import java.util.UUID;

/** A product as read from rpe_catalog. Cached in Redis as JSON. */
public record CatalogProduct(UUID id, String name, String description, String status) {

    public boolean isActive() {
        return "ATIVO".equals(status);
    }
}
