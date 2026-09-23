package com.rpe.catalog.service.exception;

import lombok.Getter;

import java.util.UUID;

/**
 * Thrown when creating or renaming a product to a name that belongs to a cancelled product.
 * The caller should reactivate that product instead.
 */
@Getter
public class CancelledProductExistsException extends RuntimeException {

    private final UUID productId;

    public CancelledProductExistsException(UUID productId, String name) {
        super("A product named '" + name + "' already exists but is cancelled; reactivate that product instead");
        this.productId = productId;
    }
}
