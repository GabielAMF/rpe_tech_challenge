package com.rpe.clientmanager.exception;

/**
 * Wrong username or password. Deliberately doesn't say which one was wrong.
 */
public class InvalidCredentialsException extends CustomException {

    public InvalidCredentialsException() {
        super(ErrorCode.INVALID_CREDENTIALS, "Invalid username or password");
    }
}
