package com.rpe.catalog.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * Machine-readable error codes returned in the {@code code} field of every error response,
 * each tied to the HTTP status it is answered with.
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND),
    PRODUCT_NAME_ALREADY_EXISTS(HttpStatus.CONFLICT),
    CANCELLED_PRODUCT_EXISTS(HttpStatus.CONFLICT),
    INVALID_PRODUCT_NAME(HttpStatus.BAD_REQUEST),
    PRODUCT_ALREADY_ACTIVE(HttpStatus.UNPROCESSABLE_ENTITY),

    // Raised by the handler itself, not by a CustomException.
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST),
    DATA_CONFLICT(HttpStatus.CONFLICT),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR);

    private final HttpStatus status;
}
