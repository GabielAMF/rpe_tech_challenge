package com.rpe.clientmanager.exception;

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

    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED),
    INVALID_USERNAME(HttpStatus.BAD_REQUEST),
    USERNAME_ALREADY_EXISTS(HttpStatus.CONFLICT),

    // Raised by the security layer or the handler itself, not by a CustomException.
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED),
    FORBIDDEN(HttpStatus.FORBIDDEN),
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST),
    DATA_CONFLICT(HttpStatus.CONFLICT),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR);

    private final HttpStatus status;
}
