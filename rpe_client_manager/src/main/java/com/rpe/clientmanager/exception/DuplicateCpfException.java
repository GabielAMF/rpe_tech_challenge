package com.rpe.clientmanager.exception;

import com.rpe.clientmanager.domain.Cpf;

public class DuplicateCpfException extends BusinessRuleException {

    public DuplicateCpfException(Cpf cpf) {
        super(ErrorCode.CPF_ALREADY_EXISTS, "A customer with CPF " + cpf.masked() + " already exists");
    }
}
