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
    DEFAULT_PRODUCT_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE);

    private final HttpStatus status;
}
