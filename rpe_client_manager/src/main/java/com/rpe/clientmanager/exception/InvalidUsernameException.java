package com.rpe.clientmanager.exception;

public class InvalidUsernameException extends CustomException {

    public InvalidUsernameException() {
        super(ErrorCode.INVALID_USERNAME, "Username must not be empty");
    }
}
