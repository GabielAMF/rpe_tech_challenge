package com.rpe.cardprocessor.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * Machine-readable error codes, each tied to the HTTP status it is answered with. Errors raised while consuming
 * messages have no HTTP caller; for them the status only documents the kind of failure.
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    INVALID_CARD_PRODUCTION_REQUEST(HttpStatus.BAD_REQUEST),
    CATALOG_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE),
    DEFAULT_PRODUCT_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE),

    CARD_NOT_FOUND(HttpStatus.NOT_FOUND),

    // Raised by GlobalExceptionHandler itself, not by a CustomException.
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST),
    DATA_CONFLICT(HttpStatus.CONFLICT),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR);

    private final HttpStatus status;
}
