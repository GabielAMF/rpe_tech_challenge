package com.rpe.clientmanager.exception;

import java.util.UUID;

public class CustomerNotFoundException extends CustomException {

    public CustomerNotFoundException(UUID id) {
        super(ErrorCode.CUSTOMER_NOT_FOUND, "Customer " + id + " not found");
    }
}
