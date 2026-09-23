package com.rpe.clientmanager.exception;

public class InvalidBirthDateException extends CustomException {

    public InvalidBirthDateException() {
        super(ErrorCode.INVALID_BIRTH_DATE, "Birth date must be in the past");
    }
}
