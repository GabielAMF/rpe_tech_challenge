package com.rpe.clientmanager.exception;

import java.util.UUID;

public class CustomerAlreadyActiveException extends BusinessRuleException {

    public CustomerAlreadyActiveException(UUID id) {
        super(ErrorCode.CUSTOMER_ALREADY_ACTIVE, "Customer " + id + " is already active");
    }
}
