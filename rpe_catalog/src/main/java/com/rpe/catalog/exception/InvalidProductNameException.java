package com.rpe.catalog.exception;

/**
 * Thrown by the domain when a product name is null or empty after trimming. Normally the request
 * DTOs reject such names first; this guards every other path into {@code Product}.
 */
public class InvalidProductNameException extends CustomException {

    public InvalidProductNameException() {
        super(ErrorCode.INVALID_PRODUCT_NAME, "Product name must not be empty");
    }
}
