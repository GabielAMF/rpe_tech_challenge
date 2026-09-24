package com.rpe.cardprocessor.service;

import java.util.Optional;
import java.util.UUID;

/** Reads products from rpe_catalog. */
public interface CatalogGateway {

    /**
     * @return the product, or empty if the catalog doesn't know this id
     * @throws com.rpe.cardprocessor.exception.CatalogUnavailableException if the catalog can't be reached
     */
    Optional<CatalogProduct> findProduct(UUID id);
}
