package com.rpe.clientmanager.exception;

public class InvalidCustomerNameException extends CustomException {

    public InvalidCustomerNameException() {
        super(ErrorCode.INVALID_CUSTOMER_NAME, "Customer name must not be empty");
    }
}
