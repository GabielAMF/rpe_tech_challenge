package com.rpe.catalog.exception;

import java.util.UUID;

/**
 * Business rule: only a cancelled product can be activated.
 */
public class ProductAlreadyActiveException extends BusinessRuleException {

    public ProductAlreadyActiveException(UUID id) {
        super(ErrorCode.PRODUCT_ALREADY_ACTIVE, "Product " + id + " is already active");
    }
}
