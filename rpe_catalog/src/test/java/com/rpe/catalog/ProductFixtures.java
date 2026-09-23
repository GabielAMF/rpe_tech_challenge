package com.rpe.catalog;

import com.rpe.catalog.domain.Product;
import com.rpe.catalog.domain.ProductName;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.UUID;

/** Builds products as if they had been loaded from the database (id and audit timestamps set). */
public final class ProductFixtures {

    public static final Instant NOW = Instant.parse("2026-09-23T12:00:00Z");

    private ProductFixtures() {
    }

    public static Product product(UUID id, String name) {
        return product(id, name, null);
    }

    public static Product product(UUID id, String name, String description) {
        Product product = new Product(new ProductName(name), description);
        ReflectionTestUtils.setField(product, "id", id);
        ReflectionTestUtils.setField(product, "createdAt", NOW);
        ReflectionTestUtils.setField(product, "updatedAt", NOW);
        return product;
    }

    public static Product cancelledProduct(UUID id, String name) {
        Product product = product(id, name);
        product.cancel();
        return product;
    }
}
