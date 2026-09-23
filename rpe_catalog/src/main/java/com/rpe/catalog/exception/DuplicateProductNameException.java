package com.rpe.catalog.exception;

public class DuplicateProductNameException extends BusinessRuleException {

    public DuplicateProductNameException(String name) {
        super(ErrorCode.PRODUCT_NAME_ALREADY_EXISTS, "A product named '" + name + "' already exists");
    }
}
