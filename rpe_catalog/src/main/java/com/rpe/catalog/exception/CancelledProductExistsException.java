package com.rpe.catalog.exception;

import lombok.Getter;

import java.util.Map;
import java.util.UUID;

/**
 * Business rule: a cancelled product's name can't be reused, whether creating or renaming a product.
 * The caller should reactivate that product instead; its id is returned as {@code productId}.
 */
@Getter
public class CancelledProductExistsException extends BusinessRuleException {

    private final UUID productId;

    public CancelledProductExistsException(UUID productId, String name) {
        super(ErrorCode.CANCELLED_PRODUCT_EXISTS,
                "A product named '" + name + "' already exists but is cancelled; reactivate that product instead");
        this.productId = productId;
    }

    @Override
    public Map<String, Object> getProperties() {
        return Map.of("productId", productId);
    }
}
