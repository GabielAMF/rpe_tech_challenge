package com.rpe.catalog.exception;

import java.util.UUID;

public class ProductNotFoundException extends CustomException {

    public ProductNotFoundException(UUID id) {
        super(ErrorCode.PRODUCT_NOT_FOUND, "Product " + id + " not found");
    }
}
