package com.rpe.catalog.exception;

import lombok.Getter;

import java.util.Map;

/**
 * Base class for every exception this service throws on purpose. GlobalExceptionHandler turns any
 * subclass into a problem-detail response using its {@link ErrorCode} and {@link #getProperties()}.
 */
@Getter
public abstract class CustomException extends RuntimeException {

    private final ErrorCode errorCode;

    protected CustomException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    /** Extra fields added to the error response body. Override to expose details such as an id. */
    public Map<String, Object> getProperties() {
        return Map.of();
    }
}
