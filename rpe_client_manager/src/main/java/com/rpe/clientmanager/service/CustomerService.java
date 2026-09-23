package com.rpe.clientmanager.service;

import com.rpe.clientmanager.domain.Cpf;
import com.rpe.clientmanager.domain.Customer;
import com.rpe.clientmanager.domain.CustomerStatus;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Customer use cases. Works with domain types only, so it doesn't depend on the web layer.
 */
public interface CustomerService {

    /** @throws com.rpe.clientmanager.exception.CustomerNotFoundException if no customer has this id */
    Customer findById(UUID id);

    /** Creates an ATIVO customer. @throws com.rpe.clientmanager.exception.BusinessRuleException if the CPF is taken */
    Customer create(String name, Cpf cpf, LocalDate birthDate);

    /**
     * Changes name and birth date. {@code status} is optional and may only be BLOQUEADO (or the current status,
     * which changes nothing); cancelling and activating have their own operations.
     */
    Customer update(UUID id, String name, LocalDate birthDate, CustomerStatus status);

    /** Soft delete: marks the customer CANCELADO. Idempotent. */
    void cancel(UUID id);

    /** BLOQUEADO or CANCELADO → ATIVO. @throws com.rpe.clientmanager.exception.CustomerAlreadyActiveException if already ATIVO */
    Customer activate(UUID id);
}
