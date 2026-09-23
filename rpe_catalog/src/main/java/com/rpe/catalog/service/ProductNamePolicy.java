package com.rpe.catalog.service;

import com.rpe.catalog.domain.ProductName;
import com.rpe.catalog.domain.ProductStatus;
import com.rpe.catalog.exception.CancelledProductExistsException;
import com.rpe.catalog.exception.DuplicateProductNameException;
import com.rpe.catalog.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Business rule: a product name is unique across all products, active or cancelled. When the name
 * belongs to a cancelled product, the caller is told to reactivate that product instead.
 */
@Component
@RequiredArgsConstructor
public class ProductNamePolicy {

    private final ProductRepository productRepository;

    public void ensureAvailableForNewProduct(ProductName name) {
        ensureAvailable(name, null);
    }

    /** Same rule, but the product being renamed may keep its own name. */
    public void ensureAvailableForRename(ProductName name, UUID productId) {
        ensureAvailable(name, productId);
    }

    private void ensureAvailable(ProductName name, UUID productId) {
        productRepository.findByName(name.value())
                .filter(existing -> productId == null || !productId.equals(existing.getId()))
                .ifPresent(existing -> {
                    if (existing.getStatus() == ProductStatus.CANCELADO) {
                        throw new CancelledProductExistsException(existing.getId(), name.value());
                    }
                    throw new DuplicateProductNameException(name.value());
                });
    }
}
