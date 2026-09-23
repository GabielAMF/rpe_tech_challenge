package com.rpe.catalog.exception;

/**
 * A request that is well-formed but breaks a business rule (e.g. reusing a product name).
 */
public abstract class BusinessRuleException extends CustomException {

    protected BusinessRuleException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
