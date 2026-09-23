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

    /**
     * The customer plus its card and product from rpe_card_processor. Card info is best effort: if that service
     * can't be reached the customer is still returned, with the lookup marked unavailable.
     */
    CustomerDetails getDetails(UUID id);

    /**
     * Creates an ATIVO customer and requests card production for it. {@code creditInfo} only travels in the
     * card production request; it is not stored here.
     *
     * @throws com.rpe.clientmanager.exception.BusinessRuleException if the CPF is taken
     * @throws com.rpe.clientmanager.exception.CardProductionUnavailableException if the request can't be sent
     *         (the customer is then not created)
     */
    Customer create(String name, Cpf cpf, LocalDate birthDate, String creditInfo);

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
