package com.rpe.clientmanager.service;

import com.rpe.clientmanager.domain.Cpf;
import com.rpe.clientmanager.domain.CustomerStatus;
import com.rpe.clientmanager.exception.CancelledCustomerExistsException;
import com.rpe.clientmanager.exception.DuplicateCpfException;
import com.rpe.clientmanager.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Business rule: a CPF belongs to exactly one customer, whatever its status. When that customer is cancelled
 * the caller is told to reactivate it instead of creating a new one.
 */
@Component
@RequiredArgsConstructor
public class CustomerCpfPolicy {

    private final CustomerRepository customerRepository;

    public void ensureAvailable(Cpf cpf) {
        customerRepository.findByCpf(cpf.value()).ifPresent(existing -> {
            if (existing.getStatus() == CustomerStatus.CANCELADO) {
                throw new CancelledCustomerExistsException(existing.getId(), cpf);
            }
            throw new DuplicateCpfException(cpf);
        });
    }
}
