package com.rpe.clientmanager.exception;

/** The message never echoes the value: it may be a (mistyped) real CPF. */
public class InvalidCpfException extends CustomException {

    public InvalidCpfException() {
        super(ErrorCode.INVALID_CPF, "CPF must have 11 letters or digits (dots, dashes and spaces are ignored)");
    }
}
