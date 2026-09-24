package com.rpe.cardprocessor.service;

import com.rpe.cardprocessor.config.CardProperties;
import com.rpe.cardprocessor.exception.DefaultProductUnavailableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Decides which catalog product a new card is issued for.
 * <p>
 * <b>PLACEHOLDER RULE.</b> There is no real credit analysis yet. For now {@code creditInfo} is read as a catalog
 * product id: if it is a UUID of a product that exists and is ATIVO, that product is used; otherwise (not a UUID,
 * unknown, or not ATIVO) the default product ({@code app.card.default-product-id}, GOLD) is used. Replace this
 * class when the business defines how credit information maps to a product.
 */
@Slf4j
@Component
public class ProductSelectionPolicy {

    private final CatalogGateway catalog;
    private final UUID defaultProductId;

    public ProductSelectionPolicy(CatalogGateway catalog, CardProperties properties) {
        this.catalog = catalog;
        this.defaultProductId = properties.defaultProductId();
    }

    public CatalogProduct select(String creditInfo) {
        Optional<CatalogProduct> requested = parseProductId(creditInfo)
                .flatMap(catalog::findProduct)
                .filter(CatalogProduct::isActive);
        if (requested.isPresent()) {
            return requested.get();
        }
        log.debug("credit_info names no usable product; using default product id={}", defaultProductId);
        return catalog.findProduct(defaultProductId)
                .filter(CatalogProduct::isActive)
                .orElseThrow(() -> new DefaultProductUnavailableException(defaultProductId));
    }

    private static Optional<UUID> parseProductId(String creditInfo) {
        try {
            return creditInfo == null ? Optional.empty() : Optional.of(UUID.fromString(creditInfo.trim()));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }
}
