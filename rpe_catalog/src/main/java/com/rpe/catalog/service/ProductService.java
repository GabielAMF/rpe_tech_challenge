package com.rpe.catalog.service;

import com.rpe.catalog.domain.Product;
import com.rpe.catalog.domain.ProductName;

import java.util.UUID;

/**
 * Product use cases. Works with domain types only, so it doesn't depend on the web layer.
 */
public interface ProductService {

    /** @throws com.rpe.catalog.exception.ProductNotFoundException if no product has this id */
    Product findById(UUID id);

    /** Creates an ATIVO product. @throws com.rpe.catalog.exception.BusinessRuleException if the name is taken */
    Product create(ProductName name, String description);

    /** Changes name and description only; status changes go through {@link #cancel} and {@link #activate}. */
    Product update(UUID id, ProductName name, String description);

    /** Soft delete: marks the product CANCELADO. Idempotent. */
    void cancel(UUID id);

    /** Reactivates a cancelled product. @throws com.rpe.catalog.exception.ProductAlreadyActiveException if already ATIVO */
    Product activate(UUID id);
}
