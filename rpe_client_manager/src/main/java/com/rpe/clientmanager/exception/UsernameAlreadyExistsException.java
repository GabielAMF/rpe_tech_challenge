package com.rpe.clientmanager.exception;

public class UsernameAlreadyExistsException extends BusinessRuleException {

    public UsernameAlreadyExistsException(String username) {
        super(ErrorCode.USERNAME_ALREADY_EXISTS, "Username '" + username + "' is already taken");
    }
}
