package com.rpe.cardprocessor.exception;

/** rpe_catalog couldn't be reached. The message is retried (and goes to the DLQ after 3 attempts). */
public class CatalogUnavailableException extends CustomException {

    public CatalogUnavailableException(Throwable cause) {
        super(ErrorCode.CATALOG_UNAVAILABLE, "rpe_catalog is unavailable");
        initCause(cause);
    }
}
