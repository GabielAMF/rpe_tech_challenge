package com.rpe.clientmanager.exception;

public class CustomerUnderageException extends BusinessRuleException {

    public CustomerUnderageException(int minimumAge) {
        super(ErrorCode.CUSTOMER_UNDERAGE, "Customer must be at least " + minimumAge + " years old");
    }
}
